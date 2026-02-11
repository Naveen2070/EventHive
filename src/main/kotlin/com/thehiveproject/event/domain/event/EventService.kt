package com.thehiveproject.event.domain.event

import com.thehiveproject.event.api.dto.CreateEventRequest
import com.thehiveproject.event.api.dto.EventDTO
import com.thehiveproject.event.api.dto.EventSearchCriteria
import com.thehiveproject.event.api.dto.UpdateEventRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EventService {
    fun createEvent(request: CreateEventRequest): Event
    fun getAllEvents(pageable: Pageable, criteria: EventSearchCriteria): Page<EventDTO>
    fun getEventById(id: Long): EventDTO
    fun getMyEvents(organizerEmail: String, pageable: Pageable): Page<EventDTO>
    fun updateEvent(eventId: Long, request: UpdateEventRequest, userEmail: String, isAdmin: Boolean): EventDTO
    fun changeEventStatus(eventId: Long, status: EventStatus, userEmail: String, isAdmin: Boolean): EventDTO
    fun deleteEvent(eventId: Long, userEmail: String, isAdmin: Boolean)
}