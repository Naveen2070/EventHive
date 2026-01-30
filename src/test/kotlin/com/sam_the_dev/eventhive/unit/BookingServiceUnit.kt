package com.sam_the_dev.eventhive.unit

import com.sam_the_dev.eventhive.api.dto.CreateBookingRequest
import com.sam_the_dev.eventhive.api.dto.PaymentWebhookPayload
import com.sam_the_dev.eventhive.application.booking.BookingServiceImpl
import com.sam_the_dev.eventhive.domain.booking.BookingStatus
import com.sam_the_dev.eventhive.domain.booking.error.InsufficientSeatsException
import com.sam_the_dev.eventhive.domain.booking.error.ResourceAccessDeniedException
import com.sam_the_dev.eventhive.domain.booking.event.BookingSuccessEvent
import com.sam_the_dev.eventhive.domain.event.EventStatus
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.BookingEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.BookingRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class BookingServiceImplUnitTest {

    @Mock
    lateinit var bookingRepository: BookingRepository

    @Mock
    lateinit var eventRepository: EventRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var eventPublisher: ApplicationEventPublisher

    @InjectMocks
    lateinit var bookingService: BookingServiceImpl

    // --- Helpers ---
    private fun createUser() = UserEntity(
        id = 1L,
        username = "fan",
        email = "fan@test.com",
        password = "x",
        createdBy = 0L,
        updatedBy = 0L,
    )

    private fun createEvent(seats: Int = 10) = EventEntity(
        id = 100L,
        title = "Concert",
        startDate = LocalDateTime.now().plusDays(5),
        endDate = LocalDateTime.now().plusDays(6),
        location = "Stadium",
        price = BigDecimal("50.00"),
        totalSeats = seats,
        availableSeats = seats, // Important
        status = EventStatus.PUBLISHED,
        organizer = createUser(),
        description = "des",
        createdBy = 0L,
        updatedBy = 0L
    )

    // --- Create Booking Tests ---

    @Test
    fun `createBooking should save booking, reduce seats, and publish event`() {
        // 1. Setup
        val user = createUser()
        val event = createEvent(seats = 10)
        val request = CreateBookingRequest(eventId = 100L, ticketsCount = 2)

        whenever(userRepository.findByUsernameOrEmail(any(), any())).thenReturn(user)
        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))

        // Mock Save: Return the entity passed to it
        whenever(bookingRepository.save(any<BookingEntity>())).thenAnswer {
            val b = it.arguments[0] as BookingEntity
            b.id = 555L
            b
        }

        // 2. Execute
        val result = bookingService.createBooking(request, "fan@test.com")

        // 3. Assert
        assertEquals(2, result.ticketsCount)
        assertEquals(BigDecimal("100.00"), result.totalPrice) // 2 * 50.00

        // Inventory Check: 10 - 2 = 8
        verify(eventRepository).save(check { savedEvent ->
            assertEquals(8, savedEvent.availableSeats)
        })

        // Event Publisher Check
        verify(eventPublisher).publishEvent(any<BookingSuccessEvent>())
    }

    @Test
    fun `createBooking should throw InsufficientSeatsException`() {
        val user = createUser()
        val event = createEvent(seats = 2) // Only 2 left
        val request = CreateBookingRequest(eventId = 100L, ticketsCount = 5) // Want 5

        whenever(userRepository.findByUsernameOrEmail(any(), any())).thenReturn(user)
        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))

        // Execute & Assert
        assertThrows(InsufficientSeatsException::class.java) {
            bookingService.createBooking(request, "fan@test.com")
        }

        // Ensure we never saved anything
        verify(bookingRepository, never()).save(any())
        verify(eventRepository, never()).save(any())
    }

    // --- Update Status Tests ---

    @Test
    fun `updateBookingStatus should restore seats when cancelling`() {
        // 1. Setup existing booking
        val user = createUser()
        val event = createEvent(seats = 5) // Currently 5 left
        val booking = BookingEntity(
            id = 1L,
            user = user,
            event = event,
            bookingReference = "REF",
            ticketsCount = 2,
            totalPrice = BigDecimal("100"),
            status = BookingStatus.CONFIRMED,
            createdBy = 0L,
            updatedBy = 0L
        )

        whenever(bookingRepository.findById(1L)).thenReturn(Optional.of(booking))
        whenever(bookingRepository.save(any<BookingEntity>())).thenAnswer { it.arguments[0] }

        // 2. Execute: User cancels their own booking
        bookingService.updateBookingStatus(1L, BookingStatus.CANCELLED, "fan@test.com", isAdmin = false)

        // 3. Assert: Seats should increase (5 + 2 = 7)
        verify(eventRepository).save(check { savedEvent ->
            assertEquals(7, savedEvent.availableSeats)
        })

        verify(bookingRepository).save(check { savedBooking ->
            assertEquals(BookingStatus.CANCELLED, savedBooking.status)
        })
    }

    @Test
    fun `updateBookingStatus user cannot upgrade to CONFIRMED`() {
        val user = createUser()
        val event = createEvent()
        val booking = BookingEntity(
            id = 1L,
            user = user,
            event = event,
            bookingReference = "REF",
            ticketsCount = 2,
            totalPrice = BigDecimal("100"),
            status = BookingStatus.PENDING_PAYMENT,
            createdBy = 0L,
            updatedBy = 0L
        )

        whenever(bookingRepository.findById(1L)).thenReturn(Optional.of(booking))

        // Execute: Regular user tries to confirm
        assertThrows(ResourceAccessDeniedException::class.java) {
            bookingService.updateBookingStatus(1L, BookingStatus.CONFIRMED, "fan@test.com", isAdmin = false)
        }
    }

    // --- Webhook Tests ---

    @Test
    fun `processPaymentWebhook should confirm booking on SUCCESS`() {
        val user = createUser()
        val event = createEvent()
        val booking = BookingEntity(
            id = 1L,
            user = user,
            event = event,
            bookingReference = "REF-123",
            ticketsCount = 1,
            totalPrice = BigDecimal("50"),
            status = BookingStatus.PENDING_PAYMENT,
            createdBy = 0L,
            updatedBy = 0L
        )

        whenever(bookingRepository.findByBookingReference("REF-123")).thenReturn(booking)

        // Execute
        val payload = PaymentWebhookPayload("REF-123", "pay_id", "SUCCESS")
        bookingService.processPaymentWebhook(payload)

        // Assert
        verify(bookingRepository).save(check { b ->
            assertEquals(BookingStatus.CONFIRMED, b.status)
        })
    }

    @Test
    fun `processPaymentWebhook should cancel booking and restore seats on FAILED`() {
        val user = createUser()
        val event = createEvent(seats = 0) // Sold out currently
        val booking = BookingEntity(
            id = 1L,
            user = user,
            event = event,
            bookingReference = "REF-123",
            ticketsCount = 2,
            totalPrice = BigDecimal("100"),
            status = BookingStatus.PENDING_PAYMENT,
            createdBy = 0L,
            updatedBy = 0L
        )

        whenever(bookingRepository.findByBookingReference("REF-123")).thenReturn(booking)

        // Execute
        val payload = PaymentWebhookPayload("REF-123", "pay_id", "FAILED")
        bookingService.processPaymentWebhook(payload)

        // Assert
        // 1. Status -> CANCELLED
        verify(bookingRepository).save(check { b ->
            assertEquals(BookingStatus.CANCELLED, b.status)
        })

        // 2. Seats Restored (0 + 2 = 2)
        verify(eventRepository).save(check { e ->
            assertEquals(2, e.availableSeats)
        })
    }
}