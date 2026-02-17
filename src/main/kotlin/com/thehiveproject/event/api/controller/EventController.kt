package com.thehiveproject.event.api.controller

import com.thehiveproject.event.api.dto.*
import com.thehiveproject.event.api.mapper.toDTO
import com.thehiveproject.event.api.utils.extractToken
import com.thehiveproject.event.domain.event.EventService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/events")
@Tag(
    name = "Events",
    description = "APIs for creating, browsing, updating, and managing events"
)
class EventController(
    private val eventService: EventService
) {

    @Operation(
        summary = "Create a new event",
        description = "Creates a new event. Only ORGANIZER, ADMIN, or SUPER_ADMIN can create events."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Event created successfully",
                content = [Content(schema = Schema(implementation = EventDTO::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid event request"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Forbidden"),
            ApiResponse(responseCode = "404", description = "Organizer not found")
        ]
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    fun createEvent(
        @RequestHeader(HttpHeaders.AUTHORIZATION) authHeader: String,
        @Parameter(description = "Event creation request payload", required = true)
        @Valid @RequestBody request: CreateEventRequest,
    ): ResponseEntity<EventDTO> {
        val token = extractToken(authHeader)
        val response = eventService.createEvent(request,token)
        val responseDTO = response.toDTO()
        return ResponseEntity(responseDTO, HttpStatus.CREATED)
    }

    @Operation(
        summary = "Get all events",
        description = """
            Retrieves a paginated list of events.
            Supports filtering by title, location, price range, date range, and status.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Events retrieved successfully",
                content = [Content(schema = Schema(implementation = PaginatedResponse::class))]
            )
        ]
    )
    @GetMapping
    fun getAllEvents(
        @Parameter(description = "Pagination information")
        @PageableDefault(size = 10, sort = ["startDate"])
        pageable: Pageable,

        @Parameter(description = "Filter by event title")
        @RequestParam(required = false) title: String?,

        @Parameter(description = "Filter by event location")
        @RequestParam(required = false) location: String?,

        @Parameter(description = "Minimum ticket price")
        @RequestParam(required = false) minPrice: BigDecimal?,

        @Parameter(description = "Maximum ticket price")
        @RequestParam(required = false) maxPrice: BigDecimal?,

        @Parameter(description = "Event start date (from)")
        @RequestParam(required = false) startDate: LocalDateTime?,

        @Parameter(description = "Event end date (to)")
        @RequestParam(required = false) endDate: LocalDateTime?,

        @Parameter(description = "Event status", example = "PUBLISHED")
        @RequestParam(required = false, defaultValue = "PUBLISHED") status: String?
    ): ResponseEntity<PaginatedResponse<EventDTO>> {
        val criteria = EventSearchCriteria(
            title = title,
            location = location,
            minPrice = minPrice,
            maxPrice = maxPrice,
            startDate = startDate,
            endDate = endDate,
            status = status
        )
        val page = eventService.getAllEvents(pageable, criteria)
        return ResponseEntity.ok(page.toPaginatedResponse())
    }

    @Operation(
        summary = "Get event by ID",
        description = "Returns a single event by its unique identifier."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Event retrieved successfully",
                content = [Content(schema = Schema(implementation = EventDTO::class))]
            ),
            ApiResponse(responseCode = "404", description = "Event not found")
        ]
    )
    @GetMapping("/{id}")
    fun getEventById(
        @Parameter(description = "Event ID", required = true)
        @PathVariable id: Long
    ): ResponseEntity<EventDTO> {
        val event = eventService.getEventById(id)
        return ResponseEntity.ok(event)
    }

    @Operation(
        summary = "Get my events",
        description = "Returns paginated events created by the authenticated organizer."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Organizer events retrieved successfully",
                content = [Content(schema = Schema(implementation = PaginatedResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Forbidden")
        ]
    )
    @GetMapping("/organizer")
    @PreAuthorize("hasRole('ORGANIZER')")
    fun getMyEvents(
        @Parameter(description = "Pagination information")
        @PageableDefault(size = 10, sort = ["createdAt"])
        @RequestHeader(HttpHeaders .AUTHORIZATION) authHeader: String,
        pageable: Pageable
    ): ResponseEntity<PaginatedResponse<EventDTO>> {
        val token = extractToken(authHeader)
        val events = eventService.getMyEvents(pageable,token)
        return ResponseEntity.ok(events.toPaginatedResponse())
    }

    @Operation(
        summary = "Update an event",
        description = """
            Updates an existing event.
            Organizers can only update their own events.
            Admins can update any event.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Event updated successfully",
                content = [Content(schema = Schema(implementation = EventDTO::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Access denied"),
            ApiResponse(responseCode = "404", description = "Event not found"),
            ApiResponse(responseCode = "409", description = "Event modification conflict")
        ]
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    fun updateEvent(
        @Parameter(description = "Event ID", required = true)
        @PathVariable id: Long,
        @Valid
        @RequestBody
        @Parameter(description = "Event update request payload", required = true)
        request: UpdateEventRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authHeader: String
    ): ResponseEntity<EventDTO> {
       val token = extractToken(authHeader)

        val response = eventService.updateEvent(id, request, token)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Change event status",
        description = """
            Changes the status of an event (PUBLISH, CANCEL, COMPLETE, etc.).
            Business rules apply if tickets are already sold.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Event status updated successfully",
                content = [Content(schema = Schema(implementation = EventDTO::class))]
            ),
            ApiResponse(responseCode = "403", description = "Access denied"),
            ApiResponse(responseCode = "404", description = "Event not found"),
            ApiResponse(responseCode = "409", description = "Invalid status transition")
        ]
    )
    @PatchMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    fun changeStatus(
        @Parameter(description = "Event ID", required = true)
        @PathVariable id: Long,
        @Valid
        @RequestBody
        @Parameter(description = "Event status change request", required = true)
        request: ChangeEventStatusRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authHeader: String
    ): ResponseEntity<EventDTO> {
       val token = extractToken(authHeader)

        val response = eventService.changeEventStatus(
            id,
            request.status,
            token
        )
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Delete an event",
        description = "Soft deletes an event. Event cannot be deleted once locked or published with sold tickets."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Event deleted successfully"),
            ApiResponse(responseCode = "403", description = "Access denied"),
            ApiResponse(responseCode = "404", description = "Event not found"),
            ApiResponse(responseCode = "409", description = "Event cannot be deleted")
        ]
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    fun deleteEvent(
        @Parameter(description = "Event ID", required = true)
        @PathVariable id: Long,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authHeader: String
    ): ResponseEntity<Void> {
        val token = extractToken(authHeader)
        eventService.deleteEvent(id,token)
        return ResponseEntity.noContent().build()
    }
}
