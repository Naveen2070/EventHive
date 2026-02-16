package com.thehiveproject.event.domain.event

import com.thehiveproject.event.api.dto.CreateEventRequest
import com.thehiveproject.event.api.dto.EventDTO
import com.thehiveproject.event.api.dto.EventSearchCriteria
import com.thehiveproject.event.api.dto.UpdateEventRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EventService {
    fun createEvent(request: CreateEventRequest, token: String): Event
    fun getAllEvents(pageable: Pageable, criteria: EventSearchCriteria): Page<EventDTO>
    fun getEventById(id: Long): EventDTO
    fun getMyEvents(pageable: Pageable, token: String): Page<EventDTO>
    fun updateEvent(
        eventId: Long,
        request: UpdateEventRequest,
        token: String
    ): EventDTO

    fun changeEventStatus(
        eventId: Long,
        status: EventStatus,
        token: String
    ): EventDTO

    fun deleteEvent(eventId: Long, token: String)
}