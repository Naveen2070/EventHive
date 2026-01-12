package com.sam_the_dev.eventhive.application.booking

import com.sam_the_dev.eventhive.api.dto.BookingDTO
import com.sam_the_dev.eventhive.api.dto.CreateBookingRequest
import com.sam_the_dev.eventhive.api.mapper.toDTO
import com.sam_the_dev.eventhive.domain.booking.Booking
import com.sam_the_dev.eventhive.domain.booking.BookingService
import com.sam_the_dev.eventhive.domain.booking.error.InsufficientSeatsException
import com.sam_the_dev.eventhive.domain.event.EventStatus
import com.sam_the_dev.eventhive.domain.event.error.EventAlreadyStartedException
import com.sam_the_dev.eventhive.domain.event.error.EventNotFoundException
import com.sam_the_dev.eventhive.domain.event.error.EventNotPublishedException
import com.sam_the_dev.eventhive.domain.user.error.UserNotFoundException
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.BookingRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.toDomain
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.toEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.event.toDomain
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.user.toDomain
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BookingServiceImpl(
    private val bookingRepository: BookingRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) : BookingService {

    private val logger = LoggerFactory.getLogger(BookingServiceImpl::class.java)

    @Transactional
    @Retryable(
        retryFor = [OptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 50)
    )
    override fun createBooking(request: CreateBookingRequest, userEmail: String): BookingDTO {
        logger.info("Attempting to book tickets for event ID: ${request.eventId}")

        // 1. Fetch User
        val userEntity = userRepository.findByUsernameOrEmail(userEmail, userEmail)
            ?: throw UserNotFoundException(userEmail, "User not found")

        // 2. Fetch Event (Fresh copy on every retry attempt)
        val eventEntity = eventRepository.findById(request.eventId)
            .orElseThrow { EventNotFoundException("Event not found") }

        // 3. Domain Logic Checks
        if (eventEntity.status != EventStatus.PUBLISHED) {
            throw EventNotPublishedException(eventEntity.id!!)
        }
        if (eventEntity.startDate.isBefore(LocalDateTime.now())) {
            throw EventAlreadyStartedException(eventEntity.id!!)
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

            logger.info("Booking successful: ${savedBooking.bookingReference}")

            // 8. Return DTO
            return booking.toDTO()
        }catch (e: Exception){
            logger.error("Failed to save booking: ${e.message}")
            throw RuntimeException("Failed to save booking: ${e.message}")
        }
    }
}