package com.thehiveproject.event.unit

import com.thehiveproject.event.api.dto.CreateEventRequest
import com.thehiveproject.event.api.dto.CreateTicketTierRequest
import com.thehiveproject.event.api.dto.UpdateEventRequest
import com.thehiveproject.event.application.event.EventServiceImpl
import com.thehiveproject.event.domain.event.EventStatus
import com.thehiveproject.event.domain.event.error.EventDateChangeNotAllowedException
import com.thehiveproject.event.domain.event.error.EventModificationNotAllowedException
import com.thehiveproject.event.domain.event.error.UnauthorizedEventAccessException
import com.thehiveproject.event.domain.user.error.UserNotFoundException
import com.thehiveproject.event.infrastructure.persistence.event.EventEntity
import com.thehiveproject.event.infrastructure.persistence.event.EventRepository
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierEntity
import com.thehiveproject.event.infrastructure.persistence.user.UserEntity
import com.thehiveproject.event.infrastructure.persistence.user.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class EventServiceUnitTest {

    @Mock
    lateinit var eventRepository: EventRepository

    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var eventService: EventServiceImpl

    // ------------------------
    // Fixtures & Helpers
    // ------------------------

    private val organizer = UserEntity(
        id = 1L,
        username = "org",
        email = "org@test.com",
        password = "x",
        createdBy = 0L,
        updatedBy = 0L
    )

    private val adminEmail = "admin@test.com"

    // Helper to create an event with attached tiers
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
            organizer = organizer,
            createdBy = 1L,
            updatedBy = 1L
        )

        // Create a tier
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
        // Setup DTO with Tiers
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

        `when`(userRepository.findByUsernameOrEmail("org@test.com", "org@test.com"))
            .thenReturn(organizer)

        `when`(eventRepository.save(any(EventEntity::class.java)))
            .thenAnswer {
                val e = it.arguments[0] as EventEntity
                e.id = 100L
                e
            }

        // Execute
        val result = eventService.createEvent(request)

        // Assert
        assertNotNull(result.id)
        assertEquals("New Event", result.title)
        assertEquals(EventStatus.DRAFT, result.status)

        // Verify the tier was added to the entity
        val captor = argumentCaptor<EventEntity>()
        verify(eventRepository).save(captor.capture())

        val savedEvent = captor.firstValue
        assertEquals(1, savedEvent.ticketTiers.size)
        assertEquals("VIP", savedEvent.ticketTiers[0].name)
    }

    @Test
    fun `should fail to create event if organizer not found`() {
        `when`(userRepository.findByUsernameOrEmail("missing@test.com", "missing@test.com"))
            .thenReturn(null)

        val tierRequest = CreateTicketTierRequest(
            name = "VIP",
            price = BigDecimal("10"),
            totalAllocation = 10,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now(),
            createdBy = 1L
        )

        assertThrows<UserNotFoundException> {
            eventService.createEvent(
                CreateEventRequest(
                    "X", "X", LocalDateTime.now(), LocalDateTime.now(), "X",
                    listOf(tierRequest), "missing@test.com", 1L
                )
            )
        }
    }

    // ------------------------
    // UPDATE EVENT
    // ------------------------

    @Test
    fun `organizer can update own event title and location`() {
        val event = createEventEntity()

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(eventRepository.save(event)).thenReturn(event)

        val result = eventService.updateEvent(
            1L,
            UpdateEventRequest("Updated", null, null, null, null),
            "org@test.com",
            false
        )

        assertEquals("Updated", result.title)
    }

    @Test
    fun `non-owner cannot update event`() {
        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(createEventEntity()))

        assertThrows<UnauthorizedEventAccessException> {
            eventService.updateEvent(
                1L,
                UpdateEventRequest("Hack", null, null, null, null),
                "hacker@test.com",
                false
            )
        }
    }

    @Test
    fun `admin can update any event`() {
        val event = createEventEntity()

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(eventRepository.save(event)).thenReturn(event)

        val result = eventService.updateEvent(
            1L,
            UpdateEventRequest("Admin Edit", null, null, null, null),
            adminEmail,
            true
        )

        assertEquals("Admin Edit", result.title)
    }

    @Test
    fun `cannot change dates if tickets sold`() {
        // Create event where available < total (means tickets sold)
        val soldEvent = createEventEntity(hasSoldTickets = true)

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(soldEvent))

        assertThrows<EventDateChangeNotAllowedException> {
            eventService.updateEvent(
                1L,
                UpdateEventRequest(null, null, null, LocalDateTime.now(), null), // Try to change start date
                "org@test.com",
                false
            )
        }
    }

    // ------------------------
    // CHANGE STATUS
    // ------------------------

    @Test
    fun `cannot revert to draft after tickets sold`() {
        val soldEvent = createEventEntity(status = EventStatus.PUBLISHED, hasSoldTickets = true)

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(soldEvent))

        assertThrows<EventModificationNotAllowedException> {
            eventService.changeEventStatus(1L, EventStatus.DRAFT, "org@test.com", false)
        }
    }

    @Test
    fun `cannot change status if completed`() {
        val completed = createEventEntity(status = EventStatus.COMPLETED)

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(completed))

        assertThrows<EventModificationNotAllowedException> {
            eventService.changeEventStatus(1L, EventStatus.CANCELLED, "org@test.com", false)
        }
    }

    @Test
    fun `should change status successfully`() {
        val event = createEventEntity(status = EventStatus.DRAFT)

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(eventRepository.save(event)).thenReturn(event)

        val result = eventService.changeEventStatus(1L, EventStatus.PUBLISHED, "org@test.com", false)

        assertEquals(EventStatus.PUBLISHED, result.status)
    }

    // ------------------------
    // DELETE EVENT
    // ------------------------

    @Test
    fun `organizer can delete unlocked event`() {
        val event = createEventEntity(status = EventStatus.DRAFT)

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(userRepository.findByUsernameOrEmail("org@test.com", "org@test.com")).thenReturn(organizer)

        eventService.deleteEvent(1L, "org@test.com", false)

        assertTrue(event.isDeleted)
        verify(eventRepository).save(event)
    }

    @Test
    fun `cannot delete published event with sold tickets`() {
        val event = createEventEntity(status = EventStatus.PUBLISHED, hasSoldTickets = true)

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(userRepository.findByUsernameOrEmail("org@test.com", "org@test.com")).thenReturn(organizer)

        val ex = assertThrows<EventModificationNotAllowedException> {
            eventService.deleteEvent(1L, "org@test.com", false)
        }
        assertTrue(ex.message!!.contains("tickets are sold"))
    }

    @Test
    fun `cannot delete published event that has started`() {
        val event = createEventEntity(status = EventStatus.PUBLISHED, hasSoldTickets = false)
        // Set start date to past
        event.startDate = LocalDateTime.now().minusHours(1)

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(userRepository.findByUsernameOrEmail("org@test.com", "org@test.com")).thenReturn(organizer)

        val ex = assertThrows<EventModificationNotAllowedException> {
            eventService.deleteEvent(1L, "org@test.com", false)
        }
        assertTrue(ex.message!!.contains("event has started"))
    }
}