package com.sam_the_dev.eventhive.api.controller

import com.sam_the_dev.eventhive.api.dto.CreateEventRequest
import com.sam_the_dev.eventhive.api.dto.EventDTO
import com.sam_the_dev.eventhive.domain.event.EventService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventService
) {

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    fun createEvent(
        @RequestBody request: CreateEventRequest,
    ): ResponseEntity<EventDTO> {
        val response = eventService.createEvent(request)
        return ResponseEntity(response, HttpStatus.CREATED)
    }
}