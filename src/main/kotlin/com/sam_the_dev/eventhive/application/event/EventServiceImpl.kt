package com.sam_the_dev.eventhive.application.event

import com.sam_the_dev.eventhive.api.dto.CreateEventRequest
import com.sam_the_dev.eventhive.api.dto.EventDTO
import com.sam_the_dev.eventhive.api.mapper.toDTO
import com.sam_the_dev.eventhive.application.user.error.UserNotFoundException
import com.sam_the_dev.eventhive.domain.event.EventService
import com.sam_the_dev.eventhive.domain.event.EventStatus
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.event.toDomain
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventServiceImpl(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) : EventService {
    private val logger = LoggerFactory.getLogger(EventServiceImpl::class.java)

    @Transactional
    override fun createEvent(request: CreateEventRequest): EventDTO {
        val organizerEmail = request.organizerEmail

        val organizer = userRepository.findByUsernameOrEmail(organizerEmail, organizerEmail)
            ?: throw UserNotFoundException(organizerEmail, "Organizer not found")


        val eventEntity = EventEntity(
            title = request.title,
            description = request.description,
            startDate = request.startDate,
            endDate = request.endDate,
            location = request.location,
            price = request.price,
            totalSeats = request.totalSeats,
            availableSeats = request.totalSeats,
            status = EventStatus.DRAFT,
            organizer = organizer,
            createdBy = request.createdBy,
            updatedBy = request.createdBy,
        )

        try {
            val savedEvent = eventRepository.save(eventEntity)
            return savedEvent.toDomain().toDTO()
        } catch (e: Exception) {
            logger.error("Failed to create event: ${e.message}")
            throw RuntimeException("Failed to create event: ${e.message}")
        }
    }
}