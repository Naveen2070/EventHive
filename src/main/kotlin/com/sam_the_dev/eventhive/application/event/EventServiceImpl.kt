package com.sam_the_dev.eventhive.application.event

import com.sam_the_dev.eventhive.api.dto.CreateEventRequest
import com.sam_the_dev.eventhive.api.dto.EventDTO
import com.sam_the_dev.eventhive.api.dto.EventSearchCriteria
import com.sam_the_dev.eventhive.api.dto.UpdateEventRequest
import com.sam_the_dev.eventhive.api.mapper.toDTO
import com.sam_the_dev.eventhive.domain.event.Event
import com.sam_the_dev.eventhive.domain.event.EventService
import com.sam_the_dev.eventhive.domain.event.EventStatus
import com.sam_the_dev.eventhive.domain.event.error.EventDateChangeNotAllowedException
import com.sam_the_dev.eventhive.domain.event.error.EventModificationNotAllowedException
import com.sam_the_dev.eventhive.domain.event.error.EventNotFoundException
import com.sam_the_dev.eventhive.domain.event.error.UnauthorizedEventAccessException
import com.sam_the_dev.eventhive.domain.user.error.UserNotFoundException
import com.sam_the_dev.eventhive.infrastructure.persistence.event.*
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.user.toDomain
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId

@Service
class EventServiceImpl(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) : EventService {
    private val logger = LoggerFactory.getLogger(EventServiceImpl::class.java)

    @Transactional
    override fun createEvent(request: CreateEventRequest): Event {
        val organizerEmail = request.organizerEmail

        val organizer = userRepository.findByUsernameOrEmail(organizerEmail, organizerEmail)
            ?: throw UserNotFoundException(organizerEmail, "Organizer not found")


        val eventEntity = EventEntity(
            title = request.title,
            description = request.description,
            startDate = request.startDate,
            endDate = request.endDate,
            location = request.location,
            status = EventStatus.DRAFT,
            organizer = organizer,
            createdBy = organizer.id ?: request.createdBy,
            updatedBy = organizer.id ?: request.createdBy,
        )

        val tiers = request.ticketTiers.map { tierReq ->
            TicketTierEntity(
                name = tierReq.name,
                price = tierReq.price,
                totalAllocation = tierReq.totalAllocation,
                availableAllocation = tierReq.totalAllocation,
                validFrom = tierReq.validFrom,
                validUntil = tierReq.validUntil,
                event = eventEntity
            )
        }

        eventEntity.ticketTiers.addAll(tiers)

        try {
            val savedEvent = eventRepository.save(eventEntity)
            return savedEvent.toDomain()
        } catch (e: Exception) {
            logger.error("Failed to create event: ${e.message}")
            throw RuntimeException("Failed to create event: ${e.message}")
        }
    }

    @Transactional(readOnly = true)
    override fun getAllEvents(pageable: Pageable, criteria: EventSearchCriteria): Page<EventDTO> {
        val specification = EventSpecification.withCriteria(criteria)
        return eventRepository.findAll(specification,pageable)
            .map { it.toDomain().toDTO() }
    }

    @Transactional(readOnly = true)
    override fun getEventById(id: Long): EventDTO{
        val event = eventRepository.findById(id)
            .orElseThrow { EventNotFoundException("Event not found with ID: $id") }

        return event.toDomain().toDTO()
    }

    @Transactional(readOnly = true)
    override fun getMyEvents(organizerEmail: String, pageable: Pageable): Page<EventDTO> {
        val organizer = userRepository.findByUsernameOrEmail(organizerEmail, organizerEmail)
            ?: throw UserNotFoundException(organizerEmail, "User not found")

        return eventRepository.findByOrganizerId(organizer.id!!, pageable)
            .map { it.toDomain().toDTO() }
    }

    @Transactional
    override fun updateEvent(eventId: Long, request: UpdateEventRequest, userEmail: String, isAdmin: Boolean): EventDTO {
        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException("Event not found") }

        // 1. Security Check: Ownership
        validateOwnership(event, userEmail, isAdmin)

        // 2. Business Rule: Cannot change dates if tickets are sold
        val hasSoldTickets = event.ticketTiers.any { it.totalAllocation != it.availableAllocation }

        if (hasSoldTickets) {
            if (request.startDate != null || request.endDate != null) {
                // If the user tries to change dates, STOP them.
                throw EventDateChangeNotAllowedException("Cannot change event dates because tickets have already been sold. Please cancel and create a new event.")
            }
        }

        // 3. Apply Updates
        request.title?.let { event.title = it }
        request.description?.let { event.description = it }
        request.location?.let { event.location = it }

        // Apply Dates (Only if no tickets sold, or if they passed the check above)
        request.startDate?.let { event.startDate = it }
        request.endDate?.let { event.endDate = it }

        try {
            val savedEvent = eventRepository.save(event)
            return savedEvent.toDomain().toDTO()
        } catch (e: Exception) {
            logger.error("Failed to update event: ${e.message}")
            throw RuntimeException("Failed to update event: ${e.message}")
        }
    }

    @Transactional
    override fun changeEventStatus(
        eventId: Long,
        status: EventStatus,
        userEmail: String,
        isAdmin: Boolean
    ): EventDTO {
        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException("Event not found") }

        validateOwnership(event, userEmail, isAdmin)

        val hasSoldTickets = event.ticketTiers.any { it.totalAllocation != it.availableAllocation }
        // Rule 1: If tickets are sold, you CANNOT go back to DRAFT.
        // But you CAN go to CANCELLED or COMPLETED.
        if (hasSoldTickets && status == EventStatus.DRAFT) {
            throw EventModificationNotAllowedException(
                "Cannot revert to DRAFT because tickets have already been sold. You must CANCEL the event instead."
            )
        }

        // Rule 2: If the event has already ended (COMPLETED), you shouldn't be able to change status anymore.
        if (event.status == EventStatus.COMPLETED) {
            throw EventModificationNotAllowedException("Cannot change status of a COMPLETED event.")
        }

        event.status = status
        try {
            val savedEvent = eventRepository.save(event)
            return savedEvent.toDomain().toDTO()
        } catch (e: Exception) {
            logger.error("Failed to change event status: ${e.message}")
            throw RuntimeException("Failed to change event status: ${e.message}")
        }
    }

    @Transactional
    override fun deleteEvent(eventId: Long, userEmail: String, isAdmin: Boolean) {
        val user = userRepository.findByUsernameOrEmail(userEmail, userEmail)
            ?: throw UserNotFoundException(userEmail, "User not found")

        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException("Event not found") }

        validateOwnership(event, userEmail, isAdmin)
        assertEventNotLocked(event)

        // Soft Delete
        event.markDeleted(user.toDomain().id!!)
        try {
            eventRepository.save(event)
        } catch (e: Exception) {
            logger.error("Failed to delete event: ${e.message}")
            throw RuntimeException("Failed to delete event: ${e.message}")
        }
    }

    private fun validateOwnership(event: EventEntity, userEmail: String, isAdmin: Boolean) {
        if (isAdmin) return // Admins can touch anything

        if (event.organizer.email != userEmail && event.organizer.username != userEmail) {
            throw UnauthorizedEventAccessException("Access Denied: You are not the organizer of this event or admin.")
        }
    }

    private fun assertEventNotLocked(event: EventEntity) {
        val hasSoldTickets = event.ticketTiers.any { it.totalAllocation != it.availableAllocation }
        val hasStarted = event.startDate.atZone(ZoneId.systemDefault()).toInstant().isBefore(Instant.now())
        val isPublished = event.status == EventStatus.PUBLISHED

        if (isPublished && (hasSoldTickets || hasStarted)) {
            throw EventModificationNotAllowedException(
                "Event cannot be modified after publishing once tickets are sold or the event has started."
            )
        }
    }

}