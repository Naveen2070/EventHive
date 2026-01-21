package com.sam_the_dev.eventhive.api.controller

import com.sam_the_dev.eventhive.api.dto.*
import com.sam_the_dev.eventhive.domain.event.EventService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventService
) {

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    fun createEvent(
        @Valid @RequestBody request: CreateEventRequest,
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

    @GetMapping("/organizer")
    @PreAuthorize("hasRole('ORGANIZER')")
    fun getMyEvents(
        authentication: Authentication,
        @PageableDefault(size = 10, sort = ["createdAt"]) pageable: Pageable
    ): ResponseEntity<PaginatedResponse<EventDTO>> {
        val events = eventService.getMyEvents(authentication.name, pageable)
        return ResponseEntity.ok(events.toPaginatedResponse())
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    fun updateEvent(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateEventRequest,
        authentication: Authentication
    ): ResponseEntity<EventDTO> {
        val isAdmin = authentication.authorities.any {
            it.authority == "ROLE_ADMIN" || it.authority == "ROLE_SUPER_ADMIN"
        }

        val response = eventService.updateEvent(id, request, authentication.name, isAdmin)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    fun changeStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: ChangeEventStatusRequest,
        authentication: Authentication
    ): ResponseEntity<EventDTO> {
        val isAdmin = authentication.authorities.any {
            it.authority == "ROLE_ADMIN" || it.authority == "ROLE_SUPER_ADMIN"
        }

        val response = eventService.changeEventStatus(id, request.status, authentication.name, isAdmin)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    fun deleteEvent(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val isAdmin = authentication.authorities.any {
            it.authority == "ROLE_ADMIN" || it.authority == "ROLE_SUPER_ADMIN"
        }

        eventService.deleteEvent(id, authentication.name, isAdmin)
        return ResponseEntity.noContent().build()
    }
}