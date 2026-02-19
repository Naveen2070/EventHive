package com.thehiveproject.event.unit

import com.thehiveproject.event.api.dto.CreateBookingRequest
import com.thehiveproject.event.api.dto.PaymentWebhookPayload
import com.thehiveproject.event.api.dto.UserSummaryDTO
import com.thehiveproject.event.application.booking.BookingServiceImpl
import com.thehiveproject.event.domain.booking.BookingStatus
import com.thehiveproject.event.domain.booking.error.InsufficientSeatsException
import com.thehiveproject.event.domain.booking.error.ResourceAccessDeniedException
import com.thehiveproject.event.domain.booking.event.BookingSuccessEvent
import com.thehiveproject.event.domain.event.EventStatus
import com.thehiveproject.event.infrastructure.persistence.booking.BookingEntity
import com.thehiveproject.event.infrastructure.persistence.booking.BookingRepository
import com.thehiveproject.event.infrastructure.persistence.client.IdentityClient
import com.thehiveproject.event.infrastructure.persistence.event.EventEntity
import com.thehiveproject.event.infrastructure.persistence.event.EventRepository
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierEntity
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierRepository
import com.thehiveproject.event.infrastructure.security.JwtService
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
class BookingServiceUnitTest {

    @Mock
    lateinit var bookingRepository: BookingRepository

    @Mock
    lateinit var eventRepository: EventRepository

    @Mock
    lateinit var ticketTierRepository: TicketTierRepository

    @Mock
    lateinit var eventPublisher: ApplicationEventPublisher

    @Mock
    lateinit var jwtService: JwtService

    @Mock
    lateinit var identityClient: IdentityClient

    @InjectMocks
    lateinit var bookingService: BookingServiceImpl

    // --- Helpers ---
    private val userId = 1L
    private val organizerId = 100L
    private val userToken = "valid-user-token"
    private val userEmail = "fan@test.com"

    private val mockOrganizer = UserSummaryDTO(organizerId, "Test Organizer", "org@test.com")

    private fun createEvent() = EventEntity(
        id = 100L,
        title = "Concert",
        startDate = LocalDateTime.now().plusDays(5),
        endDate = LocalDateTime.now().plusDays(6),
        location = "Stadium",
        status = EventStatus.PUBLISHED,
        organizerId = organizerId,
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
        val event = createEvent()
        val tier = createTier(event, seats = 10)
        val request = CreateBookingRequest(eventId = 100L, ticketTierId = 50L, ticketsCount = 2)

        // Mock JWT
        whenever(jwtService.extractUserId(userToken)).thenReturn(userId)
        whenever(jwtService.extractUsername(userToken)).thenReturn(userEmail)

        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))
        whenever(ticketTierRepository.findById(50L)).thenReturn(Optional.of(tier))

        whenever(identityClient.getUsersById(organizerId)).thenReturn(mockOrganizer)

        // Mock Save
        whenever(bookingRepository.save(any<BookingEntity>())).thenAnswer {
            val b = it.arguments[0] as BookingEntity
            b.id = 555L
            b
        }

        // 2. Execute
        val result = bookingService.createBooking(request, userToken)

        // 3. Assert
        assertEquals(2, result.ticketsCount)
        assertEquals(BigDecimal("100.00"), result.totalPrice)

        // Inventory Check on TIER: 10 - 2 = 8
        verify(ticketTierRepository).save(check { savedTier ->
            assertEquals(8, savedTier.availableAllocation)
        })

        // Event Publisher Check
        verify(eventPublisher).publishEvent(any<BookingSuccessEvent>())
    }

    @Test
    fun `createBooking should throw InsufficientSeatsException when tier is full`() {
        val event = createEvent()
        val tier = createTier(event, seats = 2)
        val request = CreateBookingRequest(eventId = 100L, ticketTierId = 50L, ticketsCount = 5)

        whenever(jwtService.extractUserId(userToken)).thenReturn(userId)
        whenever(jwtService.extractUsername(userToken)).thenReturn(userEmail)
        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))
        whenever(ticketTierRepository.findById(50L)).thenReturn(Optional.of(tier))

        // Execute & Assert
        assertThrows(InsufficientSeatsException::class.java) {
            bookingService.createBooking(request, userToken)
        }

        verify(bookingRepository, never()).save(any())
    }

    // --- Update Status Tests ---

    @Test
    fun `updateBookingStatus should restore tier seats when cancelling`() {
        // 1. Setup
        val event = createEvent()
        val tier = createTier(event, seats = 5)
        val booking = BookingEntity(
            id = 1L,
            userId = userId,
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

        // Mock Role Checks (Not Admin)
        whenever(jwtService.hasAnyRole(eq(userToken), anyVararg())).thenReturn(false)
        whenever(jwtService.extractUserId(userToken)).thenReturn(userId) // Owns the booking

        // 2. Execute
        bookingService.updateBookingStatus(1L, BookingStatus.CANCELLED, userToken)

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
        val event = createEvent()
        val tier = createTier(event)
        val booking = BookingEntity(
            id = 1L,
            userId = userId,
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
        // Mock Not Admin
        whenever(jwtService.hasAnyRole(eq(userToken), anyVararg())).thenReturn(false)
        whenever(jwtService.extractUserId(userToken)).thenReturn(userId)

        // Execute
        assertThrows(ResourceAccessDeniedException::class.java) {
            bookingService.updateBookingStatus(1L, BookingStatus.CONFIRMED, userToken)
        }
    }

    // --- Webhook Tests ---
    @Test
    fun `processPaymentWebhook should confirm booking on SUCCESS`() {
        val event = createEvent()
        val tier = createTier(event)
        val booking = BookingEntity(
            id = 1L,
            userId = userId,
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
}