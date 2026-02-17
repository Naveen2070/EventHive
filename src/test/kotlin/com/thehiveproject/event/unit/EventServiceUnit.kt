package com.thehiveproject.event.unit

import com.thehiveproject.event.api.dto.CreateEventRequest
import com.thehiveproject.event.api.dto.CreateTicketTierRequest
import com.thehiveproject.event.api.dto.UpdateEventRequest
import com.thehiveproject.event.application.event.EventServiceImpl
import com.thehiveproject.event.domain.event.EventStatus
import com.thehiveproject.event.domain.event.error.UnauthorizedEventAccessException
import com.thehiveproject.event.infrastructure.persistence.event.EventEntity
import com.thehiveproject.event.infrastructure.persistence.event.EventRepository
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierEntity
import com.thehiveproject.event.infrastructure.security.JwtService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class EventServiceUnitTest {

    @Mock
    lateinit var eventRepository: EventRepository

    @Mock
    lateinit var jwtService: JwtService

    @InjectMocks
    lateinit var eventService: EventServiceImpl

    private val organizerId = 1L
    private val organizerToken = "org-token"
    private val adminToken = "admin-token"

    // Helper to create entity
    private fun createEventEntity(
        status: EventStatus = EventStatus.DRAFT,
        hasSoldTickets: Boolean = false
    ): EventEntity {
        val event = EventEntity(
            id = 1L,
            title = "Event",
            description = "Desc",
            startDate = LocalDateTime.now().plusDays(10),
            endDate = LocalDateTime.now().plusDays(12),
            location = "NYC",
            status = status,
            organizerId = organizerId,
            createdBy = 1L,
            updatedBy = 1L
        )
        val total = 50
        val available = if (hasSoldTickets) 40 else 50
        val tier = TicketTierEntity(
            id = 10L,
            name = "General",
            price = BigDecimal("100.00"),
            totalAllocation = total,
            availableAllocation = available,
            validFrom = event.startDate,
            validUntil = event.endDate,
            event = event,
            createdBy = 1L,
            updatedBy = 1L
        )
        event.ticketTiers.add(tier)
        return event
    }

    // ------------------------
    // CREATE EVENT
    // ------------------------
    @Test
    fun `should create event successfully`() {
        val tierRequest = CreateTicketTierRequest(
            name = "VIP",
            price = BigDecimal("100"),
            totalAllocation = 50,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(1),
            createdBy = 1L
        )
        val request = CreateEventRequest(
            title = "New Event",
            description = "Desc",
            startDate = LocalDateTime.now().plusDays(1),
            endDate = LocalDateTime.now().plusDays(2),
            location = "NYC",
            ticketTiers = listOf(tierRequest),
            organizerEmail = "org@test.com",
            createdBy = 1L
        )

        `when`(jwtService.extractUserId(organizerToken)).thenReturn(organizerId)
        `when`(eventRepository.save(any<EventEntity>())).thenAnswer {
            (it.arguments[0] as EventEntity).apply { id = 100L }
        }

        val result = eventService.createEvent(request, organizerToken)

        assertNotNull(result.id)
        assertEquals("New Event", result.title)
        assertEquals(organizerId, result.organizerId)
        verify(eventRepository).save(any<EventEntity>())
    }

    // ------------------------
    // UPDATE EVENT
    // ------------------------

    @Test
    fun `organizer can update own event`() {
        val event = createEventEntity()
        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(eventRepository.save(event)).thenReturn(event)

        `when`(jwtService.extractUserId(organizerToken)).thenReturn(organizerId)

        `when`(jwtService.hasAnyRole(
            eq(organizerToken),
            eq("ROLE_ADMIN"),
            eq("ROLE_SUPER_ADMIN")
        )).thenReturn(false)

        val result = eventService.updateEvent(
            1L,
            UpdateEventRequest("Updated", null, null, null, null),
            organizerToken
        )

        assertEquals("Updated", result.title)
    }

    @Test
    fun `non-owner cannot update event`() {
        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(createEventEntity()))

        val hackerToken = "hacker"
        `when`(jwtService.extractUserId(hackerToken)).thenReturn(999L)

        `when`(jwtService.hasAnyRole(
            eq(hackerToken),
            eq("ROLE_ADMIN"),
            eq("ROLE_SUPER_ADMIN")
        )).thenReturn(false)

        assertThrows<UnauthorizedEventAccessException> {
            eventService.updateEvent(
                1L,
                UpdateEventRequest("Hack", null, null, null, null),
                hackerToken
            )
        }
    }

    @Test
    fun `admin can update any event`() {
        val event = createEventEntity()
        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(eventRepository.save(event)).thenReturn(event)

        `when`(jwtService.extractUserId(adminToken)).thenReturn(999L)

        `when`(jwtService.hasAnyRole(
            eq(adminToken),
            eq("ROLE_ADMIN"),
            eq("ROLE_SUPER_ADMIN")
        )).thenReturn(true)

        val result = eventService.updateEvent(
            1L,
            UpdateEventRequest("Admin Edit", null, null, null, null),
            adminToken
        )

        assertEquals("Admin Edit", result.title)
    }

    // ------------------------
    // DELETE EVENT
    // ------------------------

    @Test
    fun `organizer can delete unlocked event`() {
        val event = createEventEntity(status = EventStatus.DRAFT)
        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(eventRepository.save(event)).thenReturn(event)

        `when`(jwtService.extractUserId(organizerToken)).thenReturn(organizerId)

        `when`(jwtService.hasAnyRole(
            eq(organizerToken),
            eq("ROLE_ADMIN"),
            eq("ROLE_SUPER_ADMIN")
        )).thenReturn(false)

        eventService.deleteEvent(1L, organizerToken)

        assertTrue(event.isDeleted)
        verify(eventRepository).save(event)
    }
}