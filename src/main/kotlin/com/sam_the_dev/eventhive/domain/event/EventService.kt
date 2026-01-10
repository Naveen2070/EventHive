package com.sam_the_dev.eventhive.domain.event

import com.sam_the_dev.eventhive.api.dto.CreateEventRequest
import com.sam_the_dev.eventhive.api.dto.EventDTO

interface EventService {
    fun createEvent(request: CreateEventRequest): EventDTO
}