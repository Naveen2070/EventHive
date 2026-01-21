package com.sam_the_dev.eventhive.domain.event

import com.sam_the_dev.eventhive.api.dto.CreateEventRequest
import com.sam_the_dev.eventhive.api.dto.EventDTO
import com.sam_the_dev.eventhive.api.dto.EventSearchCriteria
import com.sam_the_dev.eventhive.api.dto.UpdateEventRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EventService {
    fun createEvent(request: CreateEventRequest): EventDTO
    fun getAllEvents(pageable: Pageable, criteria: EventSearchCriteria): Page<EventDTO>
    fun getEventById(id: Long): EventDTO
    fun getMyEvents(organizerEmail: String, pageable: Pageable): Page<EventDTO>
    fun updateEvent(eventId: Long, request: UpdateEventRequest, userEmail: String, isAdmin: Boolean): EventDTO
    fun changeEventStatus(eventId: Long, status: EventStatus, userEmail: String, isAdmin: Boolean): EventDTO
    fun deleteEvent(eventId: Long, userEmail: String, isAdmin: Boolean)
}