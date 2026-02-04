package com.sam_the_dev.eventhive.application.booking

import com.sam_the_dev.eventhive.api.dto.BookingDTO
import com.sam_the_dev.eventhive.api.dto.CreateBookingRequest
import com.sam_the_dev.eventhive.api.dto.PaymentWebhookPayload
import com.sam_the_dev.eventhive.api.mapper.toDTO
import com.sam_the_dev.eventhive.domain.booking.*
import com.sam_the_dev.eventhive.domain.booking.error.*
import com.sam_the_dev.eventhive.domain.booking.event.BookingSuccessEvent
import com.sam_the_dev.eventhive.domain.event.EventStatus
import com.sam_the_dev.eventhive.domain.event.error.EventAlreadyStartedException
import com.sam_the_dev.eventhive.domain.event.error.EventNotFoundException
import com.sam_the_dev.eventhive.domain.event.error.EventNotPublishedException
import com.sam_the_dev.eventhive.domain.event.error.UnauthorizedEventAccessException
import com.sam_the_dev.eventhive.domain.user.error.UserNotFoundException
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.BookingRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.toDomain
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.toEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventRepository
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
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class BookingServiceImpl(
    private val bookingRepository: BookingRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
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

        // 3. Domain Logic Checks
        if (eventEntity.status != EventStatus.PUBLISHED) {
            throw EventNotPublishedException(eventEntity.title)
        }
        if (eventEntity.startDate.isBefore(LocalDateTime.now())) {
            throw EventAlreadyStartedException(eventEntity.title)
        }

        // 4. Check Availability (The logic that might fail concurrently)
        if (eventEntity.availableSeats < request.ticketsCount) {
            throw InsufficientSeatsException(request.ticketsCount, eventEntity.availableSeats)
        }

        // 5. Create Domain Object (Using your Domain Logic)
        val bookingDomain = Booking.create(
            user = userEntity.toDomain(),
            event = eventEntity.toDomain(),
            ticketsCount = request.ticketsCount,
            pricePerTicket = eventEntity.price,
            createdBy = userEntity.id!!
        )

        // 6. Update Inventory (Decrement Seats)
        // This marks the eventEntity as "dirty". Hibernate checks version upon commit.
        eventEntity.availableSeats -= request.ticketsCount

        // 7. Save to DB
        // Save Event (triggers version increment)
        try {
            eventRepository.save(eventEntity)
        } catch (e: Exception) {
            logger.error("Failed to save event: ${e.message}")
            throw RuntimeException("Failed to save event: ${e.message}")
        }

        try {
            // Convert Domain -> Entity and Save Booking
            val bookingEntity = bookingDomain.toEntity(userEntity, eventEntity)
            val savedBooking = bookingRepository.save(bookingEntity)


            val booking = savedBooking.toDomain()

            eventPublisher.publishEvent(
                BookingSuccessEvent(
                    booking = booking.toDTO(),
                    userEmail = userEmail
                )
            )

            logger.info("Booking successful: ${savedBooking.bookingReference}")

            // 8. Return DTO
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
        val event = booking.event

        // Case A: Cancelling a valid booking -> RESTORE SEATS
        if (oldStatus != BookingStatus.CANCELLED && newStatus == BookingStatus.CANCELLED) {
            event.availableSeats += booking.ticketsCount
            eventRepository.save(event)
        }

        // Case B: Re-activating a canceled booking (Admin only) -> DEDUCT SEATS
        // We must check if seats are still available!
        if (oldStatus == BookingStatus.CANCELLED && newStatus == BookingStatus.CONFIRMED) {
            if (event.availableSeats < booking.ticketsCount) {
                throw InsufficientSeatsException(booking.ticketsCount, event.availableSeats)
            }
            event.availableSeats -= booking.ticketsCount
            eventRepository.save(event)
        }

        // 3. Update & Save
        booking.status = newStatus
        val savedBooking = bookingRepository.save(booking)

        return savedBooking.toDomain().toDTO()
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
                booking.event.availableSeats += booking.ticketsCount
                eventRepository.save(booking.event)
                bookingRepository.save(booking)
                logger.info("Booking ${booking.bookingReference} cancelled due to payment failure.")
            }
        }
    }

    @Transactional
    override fun checkInAttendee(request: CheckInRequest, userEmail: String): CheckInResponse {
        // 1. Find Booking and Event
        val booking = bookingRepository.findByBookingReference(request.bookingReference)
            ?: throw BookingNotFoundException(CheckInResponse(false, "Invalid Ticket Reference").toString())


        // 2. Check Permissions (Ensure logged-in organizer owns this event)
        validateOwnership(booking.event,userEmail)

        // 3. Check Status
        if (booking.status == BookingStatus.CHECKED_IN) {
            throw TicketAlreadyUsedException(
                CheckInResponse(
                    false,
                    "Ticket already used",
                    booking.user.username,
                    "General",
                    booking.updatedAt.atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                ).toString()
            )
        }

        if (booking.status != BookingStatus.CONFIRMED) {
            throw TicketInValidException(
                CheckInResponse(false, "Ticket is ${booking.status} (Not Paid/Cancelled)").toString()
            )
        }

        // 4. Mark as Used
        booking.status = BookingStatus.CHECKED_IN
        bookingRepository.save(booking)

        return CheckInResponse(
            true,
            "Check-in Successful",
            booking.user.username,
            "General",
            booking.updatedAt.atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        )
    }

    private fun validateOwnership(event: EventEntity, userEmail: String) {

        if (event.organizer.email != userEmail && event.organizer.username != userEmail) {
            throw UnauthorizedEventAccessException("Access Denied: You are not the organizer of this event or admin.")
        }
    }
}