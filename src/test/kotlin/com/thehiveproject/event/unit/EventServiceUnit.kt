package com.thehiveproject.event.unit

import com.thehiveproject.event.api.dto.CreateEventRequest
import com.thehiveproject.event.api.dto.CreateTicketTierRequest
import com.thehiveproject.event.api.dto.UpdateEventRequest
import com.thehiveproject.event.api.dto.UserSummaryDTO
import com.thehiveproject.event.application.event.EventServiceImpl
import com.thehiveproject.event.domain.event.EventStatus
import com.thehiveproject.event.domain.event.error.UnauthorizedEventAccessException
import com.thehiveproject.event.infrastructure.persistence.client.IdentityClient
import com.thehiveproject.event.infrastructure.persistence.event.EventEntity
import com.thehiveproject.event.infrastructure.persistence.event.EventRepository
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierEntity
import com.thehiveproject.event.infrastructure.security.JwtService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class EventServiceUnitTest {

    @Mock
    lateinit var eventRepository: EventRepository

    @Mock
    lateinit var jwtService: JwtService

    @Mock
    lateinit var identityClient: IdentityClient

    @InjectMocks
    lateinit var eventService: EventServiceImpl

    private val organizerId = 1L
    private val adminId = 999L

    // Dummy User for Mocking
    private val mockOrganizer = UserSummaryDTO(organizerId, "Test Organizer", "org@test.com")

    @BeforeEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    private fun setupMockUser(userId: Long, roles: List<String> = emptyList()) {
        val authorities = roles.map { SimpleGrantedAuthority(it) }
        val auth = UsernamePasswordAuthenticationToken("user@test.com", userId, authorities)
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = auth
        SecurityContextHolder.setContext(context)
    }

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

        `when`(eventRepository.save(any<EventEntity>())).thenAnswer {
            (it.arguments[0] as EventEntity).apply { id = 100L }
        }

        `when`(identityClient.getUsersById(organizerId)).thenReturn(mockOrganizer)

        val result = eventService.createEvent(request, organizerId)

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
        setupMockUser(organizerId, listOf("events:ROLE_ORGANIZER"))
        val event = createEventEntity()
        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(eventRepository.save(event)).thenReturn(event)

        `when`(identityClient.getUsersById(organizerId)).thenReturn(mockOrganizer)

        val result = eventService.updateEvent(
            1L,
            UpdateEventRequest("Updated", null, null, null, null),
            organizerId
        )

        assertEquals("Updated", result.title)
    }

    @Test
    fun `non-owner cannot update event`() {
        setupMockUser(999L, listOf("events:ROLE_ORGANIZER"))
        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(createEventEntity()))

        assertThrows<UnauthorizedEventAccessException> {
            eventService.updateEvent(
                1L,
                UpdateEventRequest("Hack", null, null, null, null),
                999L
            )
        }
    }

    @Test
    fun `admin can update any event`() {
        setupMockUser(adminId, listOf("events:ROLE_ADMIN"))
        val event = createEventEntity()
        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(eventRepository.save(event)).thenReturn(event)

        `when`(identityClient.getUsersById(organizerId)).thenReturn(mockOrganizer)

        val result = eventService.updateEvent(
            1L,
            UpdateEventRequest("Admin Edit", null, null, null, null),
            adminId
        )

        assertEquals("Admin Edit", result.title)
    }

    // ------------------------
    // DELETE EVENT
    // ------------------------

    @Test
    fun `organizer can delete unlocked event`() {
        setupMockUser(organizerId, listOf("events:ROLE_ORGANIZER"))
        val event = createEventEntity(status = EventStatus.DRAFT)
        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(eventRepository.save(event)).thenReturn(event)

        eventService.deleteEvent(1L, organizerId)

        assertTrue(event.isDeleted)
        verify(eventRepository).save(event)
    }
}
