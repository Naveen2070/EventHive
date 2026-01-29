package com.sam_the_dev.eventhive.unit

import com.sam_the_dev.eventhive.api.dto.CreateEventRequest
import com.sam_the_dev.eventhive.api.dto.UpdateEventRequest
import com.sam_the_dev.eventhive.application.event.EventServiceImpl
import com.sam_the_dev.eventhive.domain.event.EventStatus
import com.sam_the_dev.eventhive.domain.event.error.EventDateChangeNotAllowedException
import com.sam_the_dev.eventhive.domain.event.error.EventModificationNotAllowedException
import com.sam_the_dev.eventhive.domain.event.error.InsufficientSeatCapacityException
import com.sam_the_dev.eventhive.domain.event.error.UnauthorizedEventAccessException
import com.sam_the_dev.eventhive.domain.user.error.UserNotFoundException
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
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
    // Fixtures
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

    private fun event(
        total: Int = 50,
        available: Int = 50,
        status: EventStatus = EventStatus.DRAFT
    ) = EventEntity(
        id = 1L,
        title = "Event",
        description = "Desc",
        startDate = LocalDateTime.now().plusDays(1),
        endDate = LocalDateTime.now().plusDays(2),
        location = "NYC",
        price = BigDecimal.TEN,
        totalSeats = total,
        availableSeats = available,
        status = status,
        organizer = organizer,
        createdBy = 1L,
        updatedBy = 1L
    )

    // ------------------------
    // CREATE EVENT
    // ------------------------

    @Test
    fun `should create event successfully`() {
        val request = CreateEventRequest(
            title = "New Event",
            description = "Desc",
            startDate = LocalDateTime.now().plusDays(1),
            endDate = LocalDateTime.now().plusDays(2),
            location = "NYC",
            price = BigDecimal("100"),
            totalSeats = 50,
            organizerEmail = "org@test.com",
            createdBy = 1L
        )

        `when`(
            userRepository.findByUsernameOrEmail("org@test.com", "org@test.com")
        ).thenReturn(organizer)

        `when`(eventRepository.save(any(EventEntity::class.java)))
            .thenAnswer {
                val e = it.arguments[0] as EventEntity
                e.id = 100L
                e
            }

        val result = eventService.createEvent(request)

        assertNotNull(result.id)
        assertEquals("New Event", result.title)
        assertEquals(50, result.availableSeats)
        assertEquals(EventStatus.DRAFT, result.status)
    }

    @Test
    fun `should fail to create event if organizer not found`() {
        `when`(
            userRepository.findByUsernameOrEmail("missing@test.com", "missing@test.com")
        ).thenReturn(null)

        assertThrows<UserNotFoundException> {
            eventService.createEvent(
                CreateEventRequest(
                    "X", "X",
                    LocalDateTime.now(), LocalDateTime.now(),
                    "X", BigDecimal.TEN, 10,
                    "missing@test.com", 1L
                )
            )
        }
    }

    // ------------------------
    // UPDATE EVENT
    // ------------------------

    @Test
    fun `organizer can update own event`() {
        val event = event()

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(eventRepository.save(event)).thenReturn(event)

        val result = eventService.updateEvent(
            1L,
            UpdateEventRequest("Updated", null, null, null, null, null, null),
            "org@test.com",
            false
        )

        assertEquals("Updated", result.title)
    }

    @Test
    fun `non-owner cannot update event`() {
        `when`(eventRepository.findById(1L))
            .thenReturn(Optional.of(event()))

        assertThrows<UnauthorizedEventAccessException> {
            eventService.updateEvent(
                1L,
                UpdateEventRequest("Hack", null, null, null, null, null, null),
                "hacker@test.com",
                false
            )
        }
    }

    @Test
    fun `admin can update any event`() {
        val event = event()

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(event))
        `when`(eventRepository.save(event)).thenReturn(event)

        val result = eventService.updateEvent(
            1L,
            UpdateEventRequest("Admin Edit", null, null, null, null, null, null),
            adminEmail,
            true
        )

        assertEquals("Admin Edit", result.title)
    }

    @Test
    fun `cannot change dates if tickets sold`() {
        val soldEvent = event(50, 40)

        `when`(eventRepository.findById(1L))
            .thenReturn(Optional.of(soldEvent))

        assertThrows<EventDateChangeNotAllowedException> {
            eventService.updateEvent(
                1L,
                UpdateEventRequest(null, null, null, null, null, LocalDateTime.now(), null),
                "org@test.com",
                false
            )
        }
    }

    @Test
    fun `cannot reduce seats below sold count`() {
        val soldEvent = event(50, 45)

        `when`(eventRepository.findById(1L))
            .thenReturn(Optional.of(soldEvent))

        assertThrows<InsufficientSeatCapacityException> {
            eventService.updateEvent(
                1L,
                UpdateEventRequest(null, null, null, null, 3, null, null),
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
        val soldEvent = event(50, 45, EventStatus.PUBLISHED)

        `when`(eventRepository.findById(1L))
            .thenReturn(Optional.of(soldEvent))

        assertThrows<EventModificationNotAllowedException> {
            eventService.changeEventStatus(
                1L, EventStatus.DRAFT, "org@test.com", false
            )
        }
    }

    @Test
    fun `cannot change status if completed`() {
        val completed = event(status = EventStatus.COMPLETED)

        `when`(eventRepository.findById(1L))
            .thenReturn(Optional.of(completed))

        assertThrows<EventModificationNotAllowedException> {
            eventService.changeEventStatus(
                1L, EventStatus.CANCELLED, "org@test.com", false
            )
        }
    }

    // ------------------------
    // DELETE EVENT
    // ------------------------

    @Test
    fun `organizer can delete unlocked event`() {
        val event = event()

        `when`(eventRepository.findById(1L))
            .thenReturn(Optional.of(event))

        `when`(
            userRepository.findByUsernameOrEmail("org@test.com", "org@test.com")
        ).thenReturn(organizer)

        eventService.deleteEvent(1L, "org@test.com", false)

        assertTrue(event.isDeleted)
        verify(eventRepository).save(event)
    }

    @Test
    fun `cannot delete published event with sold tickets`() {
        val event = event(50, 40, EventStatus.PUBLISHED)

        `when`(eventRepository.findById(1L))
            .thenReturn(Optional.of(event))

        `when`(
            userRepository.findByUsernameOrEmail("org@test.com", "org@test.com")
        ).thenReturn(organizer)

        assertThrows<EventModificationNotAllowedException> {
            eventService.deleteEvent(1L, "org@test.com", false)
        }
    }
}
