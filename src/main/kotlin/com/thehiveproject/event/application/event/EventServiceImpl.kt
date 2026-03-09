package com.thehiveproject.event.application.event

import com.thehiveproject.event.api.dto.*
import com.thehiveproject.event.api.mapper.toDTO
import com.thehiveproject.event.domain.event.Event
import com.thehiveproject.event.domain.event.EventService
import com.thehiveproject.event.domain.event.EventStatus
import com.thehiveproject.event.domain.event.error.EventDateChangeNotAllowedException
import com.thehiveproject.event.domain.event.error.EventModificationNotAllowedException
import com.thehiveproject.event.domain.event.error.EventNotFoundException
import com.thehiveproject.event.domain.event.error.UnauthorizedEventAccessException
import com.thehiveproject.event.infrastructure.persistence.client.IdentityClient
import com.thehiveproject.event.infrastructure.persistence.event.*
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
    private val identityClient: IdentityClient
) : EventService {
    private val logger = LoggerFactory.getLogger(EventServiceImpl::class.java)

    @Transactional
    override fun createEvent(request: CreateEventRequest, userId: Long): Event {
        val eventEntity = EventEntity(
            title = request.title,
            description = request.description,
            startDate = request.startDate,
            endDate = request.endDate,
            location = request.location,
            status = EventStatus.DRAFT,
            organizerId = userId,
            createdBy = userId,
            updatedBy = userId,
        )

        val tiers = request.ticketTiers.map { tierReq ->
            TicketTierEntity(
                name = tierReq.name,
                price = tierReq.price,
                totalAllocation = tierReq.totalAllocation,
                availableAllocation = tierReq.totalAllocation,
                validFrom = tierReq.validFrom,
                validUntil = tierReq.validUntil,
                event = eventEntity,
                createdBy = userId,
                updatedBy = userId,
            )
        }

        eventEntity.ticketTiers.addAll(tiers)

        try {
            val savedEvent = eventRepository.save(eventEntity)
            val userData = identityClient.getUsersById(savedEvent.organizerId)
            return savedEvent.toDomain(userData)
        } catch (e: Exception) {
            logger.error("Failed to create event: ${e.message}")
            throw RuntimeException("Failed to create event: ${e.message}")
        }
    }

    @Transactional(readOnly = true)
    override fun getAllEvents(pageable: Pageable, criteria: EventSearchCriteria): Page<EventDTO> {
        val specification = EventSpecification.withCriteria(criteria)
        val eventsPage = eventRepository.findAll(specification, pageable)
        val usersMap = fetchOrganizersMap(eventsPage.content)

        return eventsPage.map { event ->
            val userData = usersMap[event.organizerId]
                ?: defaultOrganizer(event.organizerId)

            event.toDomain(userData).toDTO()
        }
    }

    @Transactional(readOnly = true)
    override fun getEventById(id: Long): EventDTO {
        val event = eventRepository.findById(id)
            .orElseThrow { EventNotFoundException("Event not found with ID: $id") }
        val userData = identityClient.getUsersById(event.organizerId)
        return event.toDomain(userData).toDTO()
    }

    @Transactional(readOnly = true)
    override fun getMyEvents(pageable: Pageable, userId: Long): Page<EventDTO> {
        val eventsPage = eventRepository.findByOrganizerId(userId, pageable)
        val usersMap = fetchOrganizersMap(eventsPage.content)

        return eventsPage.map { event ->
            val userData = usersMap[event.organizerId]
                ?: defaultOrganizer(event.organizerId)

            event.toDomain(userData).toDTO()
        }
    }

    @Transactional
    override fun updateEvent(
        eventId: Long,
        request: UpdateEventRequest,
        userId: Long
    ): EventDTO {
        val isAdmin = hasAdminRole()

        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException("Event not found") }

        // 1. Security Check: Ownership
        validateOwnership(event, userId, isAdmin)

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
            val userData = identityClient.getUsersById(savedEvent.organizerId)
            return savedEvent.toDomain(userData).toDTO()
        } catch (e: Exception) {
            logger.error("Failed to update event: ${e.message}")
            throw RuntimeException("Failed to update event: ${e.message}")
        }
    }

    @Transactional
    override fun changeEventStatus(
        eventId: Long,
        status: EventStatus,
        userId: Long
    ): EventDTO {
        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException("Event not found") }

        val isAdmin = hasAdminRole()
        validateOwnership(event, userId, isAdmin)

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
            val userData = identityClient.getUsersById(savedEvent.organizerId)
            return savedEvent.toDomain(userData).toDTO()
        } catch (e: Exception) {
            logger.error("Failed to change event status: ${e.message}")
            throw RuntimeException("Failed to change event status: ${e.message}")
        }
    }

    @Transactional
    override fun deleteEvent(eventId: Long, userId: Long) {
        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException("Event not found") }

        val isAdmin = hasAdminRole()

        validateOwnership(event, userId, isAdmin)
        assertEventNotLocked(event)

        // Soft Delete
        event.markDeleted(userId)
        try {
            eventRepository.save(event)
        } catch (e: Exception) {
            logger.error("Failed to delete event: ${e.message}")
            throw RuntimeException("Failed to delete event: ${e.message}")
        }
    }

    private fun hasAdminRole(): Boolean {
        val authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication
        return authentication?.authorities?.any {
            it.authority == "events:ROLE_ADMIN" || it.authority == "events:ROLE_SUPER_ADMIN"
        } ?: false
    }

    private fun validateOwnership(event: EventEntity, userId: Long, isAdmin: Boolean) {
        if (isAdmin) return // Admins can touch anything

        if (event.organizerId != userId ) {
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

    private fun fetchOrganizersMap(events: List<EventEntity>): Map<Long, UserSummaryDTO> {
        val organizerIds = events.map { it.organizerId }.distinct()

        return identityClient.getUsersByIds(organizerIds)
            .associateBy { it.id }
    }

    private fun defaultOrganizer(organizerId: Long): UserSummaryDTO {
        return UserSummaryDTO(
            id = organizerId,
            fullName = "Unknown Organizer",
            email = "organizer@unknown.com"
        )
    }
}