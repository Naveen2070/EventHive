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
import com.sam_the_dev.eventhive.infrastructure.persistence.event.TicketTierEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.TicketTierRepository
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
    lateinit var ticketTierRepository: TicketTierRepository

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

    private fun createEvent() = EventEntity(
        id = 100L,
        title = "Concert",
        startDate = LocalDateTime.now().plusDays(5),
        endDate = LocalDateTime.now().plusDays(6),
        location = "Stadium",
        status = EventStatus.PUBLISHED,
        organizer = createUser(),
        description = "des",
        createdBy = 0L,
        updatedBy = 0L
    )

    private fun createTier(event: EventEntity, seats: Int = 10) = TicketTierEntity(
        id = 50L,
        name = "General",
        price = BigDecimal("50.00"),
        totalAllocation = 100,
        availableAllocation = seats,
        validFrom = LocalDateTime.now(),
        validUntil = LocalDateTime.now().plusDays(10),
        event = event,
        createdBy = 0L,
        updatedBy = 0L
    )

    // --- Create Booking Tests ---

    @Test
    fun `createBooking should save booking, reduce tier inventory, and publish event`() {
        // 1. Setup
        val user = createUser()
        val event = createEvent()
        val tier = createTier(event, seats = 10)

        // Request now needs ticketTierId
        val request = CreateBookingRequest(eventId = 100L, ticketTierId = 50L, ticketsCount = 2)

        whenever(userRepository.findByUsernameOrEmail(any(), any())).thenReturn(user)
        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))
        whenever(ticketTierRepository.findById(50L)).thenReturn(Optional.of(tier))

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

        // Inventory Check on TIER: 10 - 2 = 8
        verify(ticketTierRepository).save(check { savedTier ->
            assertEquals(8, savedTier.availableAllocation)
        })

        // Event should also be saved
        verify(eventRepository).save(any<EventEntity>())

        // Event Publisher Check
        verify(eventPublisher).publishEvent(any<BookingSuccessEvent>())
    }

    @Test
    fun `createBooking should throw InsufficientSeatsException when tier is full`() {
        val user = createUser()
        val event = createEvent()
        val tier = createTier(event, seats = 2)

        val request = CreateBookingRequest(eventId = 100L, ticketTierId = 50L, ticketsCount = 5)

        whenever(userRepository.findByUsernameOrEmail(any(), any())).thenReturn(user)
        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))
        whenever(ticketTierRepository.findById(50L)).thenReturn(Optional.of(tier))

        // Execute & Assert
        assertThrows(InsufficientSeatsException::class.java) {
            bookingService.createBooking(request, "fan@test.com")
        }

        // Ensure we never saved anything
        verify(bookingRepository, never()).save(any())
        verify(ticketTierRepository, never()).save(any())
    }

    // --- Update Status Tests ---

    @Test
    fun `updateBookingStatus should restore tier seats when cancelling`() {
        // 1. Setup existing booking
        val user = createUser()
        val event = createEvent()
        val tier = createTier(event, seats = 5)

        val booking = BookingEntity(
            id = 1L,
            user = user,
            event = event,
            ticketTier = tier,
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

        // 3. Assert: Tier Seats should increase (5 + 2 = 7)
        verify(ticketTierRepository).save(check { savedTier ->
            assertEquals(7, savedTier.availableAllocation)
        })

        verify(bookingRepository).save(check { savedBooking ->
            assertEquals(BookingStatus.CANCELLED, savedBooking.status)
        })
    }

    @Test
    fun `updateBookingStatus user cannot upgrade to CONFIRMED`() {
        val user = createUser()
        val event = createEvent()
        val tier = createTier(event)

        val booking = BookingEntity(
            id = 1L,
            user = user,
            event = event,
            ticketTier = tier,
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
        val tier = createTier(event)

        val booking = BookingEntity(
            id = 1L,
            user = user,
            event = event,
            ticketTier = tier,
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
    fun `processPaymentWebhook should cancel booking and restore tier seats on FAILED`() {
        val user = createUser()
        val event = createEvent()
        val tier = createTier(event, seats = 0) // Sold out currently

        val booking = BookingEntity(
            id = 1L,
            user = user,
            event = event,
            ticketTier = tier,
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

        // 2. Tier Seats Restored (0 + 2 = 2)
        verify(ticketTierRepository).save(check { t ->
            assertEquals(2, t.availableAllocation)
        })
    }
}