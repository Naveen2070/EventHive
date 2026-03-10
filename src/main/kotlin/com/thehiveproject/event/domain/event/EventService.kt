package com.thehiveproject.event.domain.event

import com.thehiveproject.event.api.dto.CreateEventRequest
import com.thehiveproject.event.api.dto.EventDTO
import com.thehiveproject.event.api.dto.EventSearchCriteria
import com.thehiveproject.event.api.dto.UpdateEventRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EventService {
    fun createEvent(request: CreateEventRequest, userId: Long): Event
    fun getAllEvents(pageable: Pageable, criteria: EventSearchCriteria): Page<EventDTO>
    fun getEventById(id: Long): EventDTO
    fun getMyEvents(pageable: Pageable, userId: Long): Page<EventDTO>
    fun updateEvent(
        eventId: Long,
        request: UpdateEventRequest,
        userId: Long
    ): EventDTO

    fun changeEventStatus(
        eventId: Long,
        status: EventStatus,
        userId: Long
    ): EventDTO

    fun deleteEvent(eventId: Long, userId: Long)
}