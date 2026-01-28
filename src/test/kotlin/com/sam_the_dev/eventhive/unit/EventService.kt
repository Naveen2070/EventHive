package com.sam_the_dev.eventhive.unit

import com.sam_the_dev.eventhive.api.dto.CreateEventRequest
import com.sam_the_dev.eventhive.api.dto.UpdateEventRequest
import com.sam_the_dev.eventhive.application.event.EventServiceImpl
import com.sam_the_dev.eventhive.domain.booking.error.ResourceAccessDeniedException
import com.sam_the_dev.eventhive.domain.event.EventStatus
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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

    private val organizer = UserEntity(
        id = 1L, username = "org", email = "org@test.com", password = "x", updatedBy = 0L, createdBy = 0L
    )

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

        // Mock dependencies
        `when`(userRepository.findByUsernameOrEmail("org@test.com", "org@test.com"))
            .thenReturn(organizer)

        // When save is called, return the entity with an ID
        `when`(eventRepository.save(any(EventEntity::class.java))).thenAnswer {
            val entity = it.arguments[0] as EventEntity
            entity.id = 100L
            entity
        }

        // Execute
        val result = eventService.createEvent(request)

        // Verify
        assertNotNull(result.id)
        assertEquals("New Event", result.title)
        assertEquals(50, result.availableSeats) // Should start equal to totalSeats
        verify(eventRepository).save(any())
    }

    @Test
    fun `should throw exception if non-owner tries to update event`() {
        val existingEvent = EventEntity(
            title = "My Event",
            organizer = organizer,
            description = "Desc",
            startDate = LocalDateTime.now(), endDate = LocalDateTime.now(), location = "X",
            price = BigDecimal.TEN,
            totalSeats = 10,
            availableSeats = 10,
            status = EventStatus.PUBLISHED,
            updatedBy = 0L,
            createdBy = 0L
        )

        `when`(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent))
        `when`(userRepository.findByUsernameOrEmail("hacker", "hacker"))
            .thenReturn(
                UserEntity(
                    id = 99L,
                    username = "hacker",
                    email = "hacker",
                    password = "x",
                    updatedBy = 0L,
                    createdBy = 0L
                )
            )

        // Execute & Assert
        assertThrows<ResourceAccessDeniedException> {
            eventService.updateEvent(
                eventId = 1L,
                request = UpdateEventRequest(title = "Hacked", null, null, null, null, null, null),
                userEmail = "hacker",
                isAdmin = false
            )
        }
    }
}