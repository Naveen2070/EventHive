package com.sam_the_dev.eventhive.api.controller

import com.sam_the_dev.eventhive.api.dto.CreateEventRequest
import com.sam_the_dev.eventhive.api.dto.EventDTO
import com.sam_the_dev.eventhive.api.dto.PaginatedResponse
import com.sam_the_dev.eventhive.api.dto.toPaginatedResponse
import com.sam_the_dev.eventhive.domain.event.EventService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
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

    @GetMapping
    fun getAllEvents(
        @PageableDefault(size = 10, sort = ["startDate"]) pageable: Pageable
    ):  ResponseEntity<PaginatedResponse<EventDTO>> {
        val page = eventService.getAllEvents(pageable)
        return ResponseEntity.ok(page.toPaginatedResponse())
    }

    @GetMapping("/{id}")
    fun getEventById(@PathVariable id: Long): ResponseEntity<EventDTO> {
        val event = eventService.getEventById(id)
        return ResponseEntity.ok(event)
    }
}