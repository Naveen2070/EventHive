package com.sam_the_dev.eventhive.domain.event

import com.sam_the_dev.eventhive.api.dto.CreateEventRequest
import com.sam_the_dev.eventhive.api.dto.EventDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EventService {
    fun createEvent(request: CreateEventRequest): EventDTO
    fun getAllEvents(pageable: Pageable): Page<EventDTO>
    fun getEventById(id: Long): EventDTO
}