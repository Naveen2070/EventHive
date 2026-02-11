package com.thehiveproject.event.unit

import com.thehiveproject.event.api.dto.CreateTicketTierRequest
import com.thehiveproject.event.api.dto.UpdateTicketTierRequest
import com.thehiveproject.event.application.event.TicketTierServiceImpl
import com.thehiveproject.event.domain.event.EventStatus
import com.thehiveproject.event.domain.event.error.InvalidTicketTierException
import com.thehiveproject.event.domain.event.error.UnauthorizedEventAccessException
import com.thehiveproject.event.infrastructure.persistence.event.EventEntity
import com.thehiveproject.event.infrastructure.persistence.event.EventRepository
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierEntity
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierRepository
import com.thehiveproject.event.infrastructure.persistence.user.UserEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class TicketTierServiceUnitTest {

    @Mock
    lateinit var ticketTierRepository: TicketTierRepository

    @Mock
    lateinit var eventRepository: EventRepository

    @InjectMocks
    lateinit var ticketTierService: TicketTierServiceImpl

    // --- Helpers ---
    private val organizerEmail = "org@test.com"
    private val otherEmail = "other@test.com"

    private fun createOrganizer() = UserEntity(
        id = 1L,
        username = "org",
        email = organizerEmail,
        password = "x",
        createdBy = 0L,
        updatedBy = 0L
    )

    private fun createEvent(
        startDate: LocalDateTime = LocalDateTime.now(),
        endDate: LocalDateTime = LocalDateTime.now().plusDays(10)
    ) = EventEntity(
        id = 100L,
        title = "Test Event",
        startDate = startDate,
        endDate = endDate,
        location = "Venue",
        organizer = createOrganizer(),
        description = "Desc",
        createdBy = 1L,
        updatedBy = 1L,
        status = EventStatus.DRAFT
    )

    private fun createTier(event: EventEntity, total: Int = 100, sold: Int = 0) = TicketTierEntity(
        id = 50L,
        name = "General",
        price = BigDecimal("50.00"),
        totalAllocation = total,
        availableAllocation = total - sold,
        validFrom = event.startDate,
        validUntil = event.endDate,
        event = event,
        createdBy = 1L,
        updatedBy = 1L
    )

    // --- Add Tier Tests ---

    @Test
    fun `addTierToEvent should succeed when valid`() {
        val event = createEvent()
        val request = CreateTicketTierRequest(
            name = "VIP",
            price = BigDecimal("100.00"),
            totalAllocation = 50,
            validFrom = event.startDate.plusDays(1),
            validUntil = event.endDate.minusDays(1),
            createdBy = 1L
        )

        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))
        whenever(ticketTierRepository.save(any<TicketTierEntity>())).thenAnswer {
            (it.arguments[0] as TicketTierEntity).apply { id = 55L }
        }

        val result = ticketTierService.addTierToEvent(100L, request, organizerEmail, false)

        assertNotNull(result)
        assertEquals("VIP", result.name)
        verify(ticketTierRepository).save(any())
    }

    @Test
    fun `addTierToEvent should fail if dates outside event range`() {
        val event = createEvent(
            startDate = LocalDateTime.now(),
            endDate = LocalDateTime.now().plusDays(5)
        )
        // Invalid: Starts before event
        val request = CreateTicketTierRequest(
            name = "VIP",
            price = BigDecimal("100"),
            totalAllocation = 50,
            validFrom = LocalDateTime.now().minusDays(1),
            validUntil = LocalDateTime.now().plusDays(5),
            createdBy = 1L
        )

        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))

        val ex = assertThrows(InvalidTicketTierException::class.java) {
            ticketTierService.addTierToEvent(100L, request, organizerEmail, false)
        }
        assertTrue(ex.message!!.contains("validity must be within"))
    }

    @Test
    fun `addTierToEvent should fail if name is duplicate`() {
        val event = createEvent()
        event.ticketTiers.add(createTier(event).apply { name = "General" }) // Existing tier

        val request = CreateTicketTierRequest(
            name = "GENERAL", // Case-insensitive check
            price = BigDecimal("100"),
            totalAllocation = 50,
            validFrom = event.startDate,
            validUntil = event.endDate,
            createdBy = 1L
        )

        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))

        val ex = assertThrows(InvalidTicketTierException::class.java) {
            ticketTierService.addTierToEvent(100L, request, organizerEmail, false)
        }
        assertTrue(ex.message!!.contains("already exists"))
    }

    @Test
    fun `addTierToEvent should fail if user is not organizer`() {
        val event = createEvent()
        val request = CreateTicketTierRequest(
            name = "VIP", price = BigDecimal("100"), totalAllocation = 50,
            validFrom = event.startDate, validUntil = event.endDate, createdBy = 1L
        )

        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))

        assertThrows(UnauthorizedEventAccessException::class.java) {
            ticketTierService.addTierToEvent(100L, request, otherEmail, false)
        }
    }

    // --- Update Tier Tests ---

    @Test
    fun `updateTier should update name and price`() {
        val event = createEvent()
        val tier = createTier(event)

        val request = UpdateTicketTierRequest(
            name = "New Name",
            price = BigDecimal("99.99"),
            updatedBy = 1L,
            totalAllocation = 100,
            validFrom = event.startDate,
            validUntil = event.endDate,
        )

        whenever(ticketTierRepository.findById(50L)).thenReturn(Optional.of(tier))
        whenever(ticketTierRepository.save(any<TicketTierEntity>())).thenAnswer { it.arguments[0] }

        val result = ticketTierService.updateTier(50L, request, organizerEmail, false)

        assertEquals("New Name", result.name)
        assertEquals(BigDecimal("99.99"), result.price)
    }

    @Test
    fun `updateTier should fail reducing allocation below sold count`() {
        val event = createEvent()
        // Total 100, Sold 20 (Available 80)
        val tier = createTier(event, total = 100, sold = 20)

        // Try to reduce total to 10 (which is less than 20 sold)
        val request = UpdateTicketTierRequest(
            totalAllocation = 10, updatedBy = 1L,
            name = "General",
            price = BigDecimal("50.00"),
            validFrom = event.startDate,
            validUntil = event.endDate,
        )

        whenever(ticketTierRepository.findById(50L)).thenReturn(Optional.of(tier))

        val ex = assertThrows(InvalidTicketTierException::class.java) {
            ticketTierService.updateTier(50L, request, organizerEmail, false)
        }
        assertTrue(ex.message!!.contains("sold"))
    }

    @Test
    fun `updateTier should correctly recalculate available allocation`() {
        val event = createEvent()
        // Total 100, Sold 20 (Available 80)
        val tier = createTier(event, total = 100, sold = 20)

        // Reduce total to 50.
        // Expect: Sold is still 20. Available should be 50 - 20 = 30.
        val request = UpdateTicketTierRequest(
            totalAllocation = 50, updatedBy = 1L,
            name = "General",
            price = BigDecimal("50.00"),
            validFrom = event.startDate,
            validUntil = event.endDate,
        )

        whenever(ticketTierRepository.findById(50L)).thenReturn(Optional.of(tier))
        whenever(ticketTierRepository.save(any<TicketTierEntity>())).thenAnswer { it.arguments[0] }

        val result = ticketTierService.updateTier(50L, request, organizerEmail, false)

        assertEquals(50, result.totalAllocation)
        assertEquals(30, result.availableAllocation)
    }

    // --- Delete Tier Tests ---

    @Test
    fun `deleteTier should succeed if no tickets sold`() {
        val event = createEvent()
        val tier = createTier(event, total = 100, sold = 0) // No sales

        whenever(ticketTierRepository.findById(50L)).thenReturn(Optional.of(tier))

        ticketTierService.deleteTier(50L, organizerEmail, false)

        verify(ticketTierRepository).delete(tier)
    }

    @Test
    fun `deleteTier should fail if tickets have been sold`() {
        val event = createEvent()
        val tier = createTier(event, total = 100, sold = 1) // 1 sale

        whenever(ticketTierRepository.findById(50L)).thenReturn(Optional.of(tier))

        val ex = assertThrows(InvalidTicketTierException::class.java) {
            ticketTierService.deleteTier(50L, organizerEmail, false)
        }
        assertTrue(ex.message!!.contains("tickets have already been sold"))
        verify(ticketTierRepository, never()).delete(any())
    }

    @Test
    fun `deleteTier should succeed for ADMIN even if not organizer`() {
        val event = createEvent()
        val tier = createTier(event, total = 100, sold = 0)

        whenever(ticketTierRepository.findById(50L)).thenReturn(Optional.of(tier))

        // Access via Other Email, but IS ADMIN
        ticketTierService.deleteTier(50L, otherEmail, true)

        verify(ticketTierRepository).delete(tier)
    }
}