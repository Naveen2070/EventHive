package com.sam_the_dev.eventhive.application.booking

import com.sam_the_dev.eventhive.api.dto.BookingDTO
import com.sam_the_dev.eventhive.api.dto.CreateBookingRequest
import com.sam_the_dev.eventhive.api.dto.PaymentWebhookPayload
import com.sam_the_dev.eventhive.api.mapper.toDTO
import com.sam_the_dev.eventhive.domain.booking.*
import com.sam_the_dev.eventhive.domain.booking.error.BookingNotFoundException
import com.sam_the_dev.eventhive.domain.booking.error.InsufficientSeatsException
import com.sam_the_dev.eventhive.domain.booking.error.ResourceAccessDeniedException
import com.sam_the_dev.eventhive.domain.booking.event.BookingSuccessEvent
import com.sam_the_dev.eventhive.domain.event.EventStatus
import com.sam_the_dev.eventhive.domain.event.error.*
import com.sam_the_dev.eventhive.domain.user.error.UserNotFoundException
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.BookingRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.toDomain
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.toEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.event.TicketTierRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.event.toDomain
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.user.toDomain
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class BookingServiceImpl(
    private val bookingRepository: BookingRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val ticketTierRepository: TicketTierRepository,
    private val eventPublisher: ApplicationEventPublisher
) : BookingService {

    private val logger = LoggerFactory.getLogger(BookingServiceImpl::class.java)

    @Transactional
    @Retryable(
        retryFor = [OptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 50)
    )
    override fun createBooking(request: CreateBookingRequest, userEmail: String): Booking {
        logger.info("Attempting to book tickets for event ID: ${request.eventId}")

        // 1. Fetch User
        val userEntity = userRepository.findByUsernameOrEmail(userEmail, userEmail)
            ?: throw UserNotFoundException(userEmail, "User not found")

        // 2. Fetch Event (Fresh copy on every retry attempt)
        val eventEntity = eventRepository.findById(request.eventId)
            .orElseThrow { EventNotFoundException("Event not found") }

        // 3. Fetch Ticket Tier (Fresh copy on every retry attempt)
        val tierEntity = ticketTierRepository.findById(request.ticketTierId)
            .orElseThrow { TicketTierNotFoundException("Ticket Tier not found") }

        // 3.1 Check if the ticket tier belongs to the event
        if (tierEntity.event.id != eventEntity.id) {
            throw InvalidTicketTierException("Ticket Tier does not belong to this Event")
        }

        // 4. Domain Logic Checks
        if (eventEntity.status != EventStatus.PUBLISHED) {
            throw EventNotPublishedException(eventEntity.title)
        }
        if (eventEntity.endDate.isBefore(LocalDateTime.now())) {
            throw EventAlreadyStartedException("Event has ended")
        }

        // 5. Check Availability (The logic that might fail concurrently)
        if (tierEntity.availableAllocation < request.ticketsCount) {
            throw InsufficientSeatsException(request.ticketsCount, tierEntity.availableAllocation)
        }

        // 6. Create Domain Object (Using your Domain Logic)
        val bookingDomain = Booking.create(
            user = userEntity.toDomain(),
            event = eventEntity.toDomain(),
            tier = tierEntity,
            ticketsCount = request.ticketsCount,
            createdBy = userEntity.id!!
        )

        // 7. Update Inventory (Decrement Seats)
        // This marks the tierEntity as "dirty". Hibernate checks version upon commit.
        tierEntity.availableAllocation -= request.ticketsCount

        // 8. Save to DB
        // Save Event (triggers version increment)
        try {
            ticketTierRepository.save(tierEntity)
            eventRepository.save(eventEntity)
        } catch (e: Exception) {
            logger.error("Failed to save event: ${e.message}")
            throw RuntimeException("Failed to save event: ${e.message}")
        }

        try {
            // Convert Domain -> Entity and Save Booking
            val bookingEntity = bookingDomain.toEntity(userEntity, eventEntity, tierEntity)
            val savedBooking = bookingRepository.save(bookingEntity)


            val booking = savedBooking.toDomain()

            eventPublisher.publishEvent(
                BookingSuccessEvent(
                    booking = booking.toDTO(),
                    userEmail = userEmail
                )
            )

            logger.info("Booking successful: ${savedBooking.bookingReference}")

            // 9. Return DTO
            return booking
        } catch (e: Exception) {
            logger.error("Failed to save booking: ${e.message}")
            throw RuntimeException("Failed to save booking: ${e.message}")
        }
    }

    @Transactional(readOnly = true)
    override fun getMyBookings(userEmail: String, pageable: Pageable): Page<BookingDTO> {
        // 1. Get the User ID from the email
        val user = userRepository.findByUsernameOrEmail(userEmail, userEmail)
            ?: throw UserNotFoundException(userEmail, "User not found")

        // 2. Fetch Bookings from Repository
        val bookingsPage = bookingRepository.findByUserId(user.id!!, pageable)

        // 3. Map Entity -> DTO
        return bookingsPage.map { booking ->
            booking.toDomain().toDTO()
        }
    }

    @Transactional
    override fun updateBookingStatus(
        bookingId: Long,
        newStatus: BookingStatus,
        userEmail: String,
        isAdmin: Boolean
    ): BookingDTO {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { BookingNotFoundException("Booking not found") }

        // 1. Security Check
        if (!isAdmin) {
            // Regular users can ONLY Cancel their own bookings
            if (booking.user.email != userEmail && booking.user.username != userEmail) {
                throw ResourceAccessDeniedException("You can only modify your own bookings.")
            }
            if (newStatus != BookingStatus.CANCELLED) {
                throw ResourceAccessDeniedException("Users can only CANCEL bookings. Other updates require Admin privileges.")
            }
        }

        // 2. Logic: Handle Inventory Changes
        val oldStatus = booking.status
        val tier = booking.ticketTier

        try {
            // Case A: Cancelling a valid booking -> RESTORE SEATS
            if (oldStatus != BookingStatus.CANCELLED && newStatus == BookingStatus.CANCELLED) {
                tier.availableAllocation += booking.ticketsCount
                ticketTierRepository.save(tier)
            }

            // Case B: Re-activating a canceled booking (Admin only) -> DEDUCT SEATS
            // We must check if seats are still available!
            if (oldStatus == BookingStatus.CANCELLED && newStatus == BookingStatus.CONFIRMED) {
                if (tier.availableAllocation < booking.ticketsCount) {
                    throw InsufficientSeatsException(booking.ticketsCount, tier.availableAllocation)
                }
                tier.availableAllocation -= booking.ticketsCount
                ticketTierRepository.save(tier)
            }

            // 3. Update & Save
            booking.status = newStatus
            val savedBooking = bookingRepository.save(booking)

            return savedBooking.toDomain().toDTO()
        }catch (e: Exception){
            logger.error("Failed to update booking status: ${e.message}")
            throw RuntimeException("Failed to update booking status: ${e.message}")
        }

    }

    @Transactional
    override fun processPaymentWebhook(payload: PaymentWebhookPayload) {
        logger.info("Received webhook for booking: ${payload.bookingReference}")

        val booking = bookingRepository.findByBookingReference(payload.bookingReference)
            ?: throw RuntimeException("Booking not found for reference: ${payload.bookingReference}")

        if (payload.status == "SUCCESS") {
            if (booking.status == BookingStatus.PENDING_PAYMENT) {
                booking.status = BookingStatus.CONFIRMED
                bookingRepository.save(booking)
                logger.info("Booking ${booking.bookingReference} confirmed via Webhook.")
            }
        } else if (payload.status == "FAILED") {
            // Payment failed -> Cancel booking and free up seats
            if (booking.status != BookingStatus.CANCELLED) {
                booking.status = BookingStatus.CANCELLED
                val tier = booking.ticketTier
                tier.availableAllocation += booking.ticketsCount
                ticketTierRepository.save(tier)
                bookingRepository.save(booking)
                logger.info("Booking ${booking.bookingReference} cancelled due to payment failure. Inventory restored to tier ${tier.name}.")
            }
        }
    }

    @Transactional
    override fun checkInAttendee(request: CheckInRequest, userEmail: String): CheckInResponse {
        val booking = bookingRepository.findByBookingReference(request.bookingReference)
            ?: throw BookingNotFoundException("Invalid Ticket Reference")

        // 1. Ownership Check
        if (booking.event.organizer.email != userEmail && booking.event.organizer.username != userEmail) {
            throw UnauthorizedEventAccessException("Access Denied")
        }

        val now = LocalDateTime.now()
        val tier = booking.ticketTier

        // 2. Validate Payment Status
        if (booking.status != BookingStatus.CONFIRMED && booking.status != BookingStatus.CHECKED_IN) {
            return CheckInResponse(false, "Ticket status is ${booking.status}", booking.user.username, tier.name)
        }

        // 3. Date/Time Validation (Multi-Day Logic)
        if (now.isBefore(tier.validFrom)) {
            return CheckInResponse(false, "Ticket valid from ${tier.validFrom}", booking.user.username, tier.name)
        }
        if (now.isAfter(tier.validUntil)) {
            booking.status = BookingStatus.EXPIRED
            bookingRepository.save(booking)
            return CheckInResponse(false, "Ticket Expired", booking.user.username, tier.name)
        }

        // 4. Re-Entry Logic
        // Convert Instant to LocalDate (system default zone) to check "Same Day"
        val lastCheckInInstant = booking.lastCheckedInAt
        val isReEntry = if (lastCheckInInstant != null) {
            val lastDate = lastCheckInInstant.atZone(ZoneId.systemDefault()).toLocalDate()
            val today = now.toLocalDate()
            lastDate.isEqual(today)
        } else false

        if (isReEntry) {
            return CheckInResponse(
                true,
                "Re-entry Verified (Already checked in today)",
                booking.user.username,
                tier.name,
                LocalDateTime.ofInstant(booking.lastCheckedInAt, ZoneId.systemDefault())
            )
        }

        // 5. Process New Check-in
        try {
            booking.status = BookingStatus.CHECKED_IN
            booking.lastCheckedInAt = Instant.now()
            booking.checkInCount += 1
            bookingRepository.save(booking)
        } catch (e: Exception) {
            logger.error("Failed to check in attendee: ${e.message}")
            throw RuntimeException("Failed to check in attendee: ${e.message}")
        }

        return CheckInResponse(
            true,
            "Check-in Successful",
            booking.user.username,
            tier.name,
            LocalDateTime.now()
        )
    }

}