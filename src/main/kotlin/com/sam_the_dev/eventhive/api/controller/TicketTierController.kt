package com.sam_the_dev.eventhive.api.controller

import com.sam_the_dev.eventhive.api.dto.CreateTicketTierRequest
import com.sam_the_dev.eventhive.api.dto.TicketTierDTO
import com.sam_the_dev.eventhive.api.dto.UpdateTicketTierRequest
import com.sam_the_dev.eventhive.api.mapper.toDTO
import com.sam_the_dev.eventhive.domain.event.TicketTierService
import com.sam_the_dev.eventhive.domain.role.UserRole
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tiers")
@Tag(name = "Ticket Tiers", description = "Manage ticket tiers for events")
@SecurityRequirement(name = "bearerAuth")
class TicketTierController(
    private val ticketTierService: TicketTierService
) {

    @PostMapping("/events/{eventId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(
        summary = "Add ticket tier to event",
        description = "Creates a new ticket tier for an event. Only the organizer or admins can perform this action."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Ticket tier created successfully"),
            ApiResponse(responseCode = "400", description = "Invalid ticket tier data"),
            ApiResponse(responseCode = "403", description = "Access denied"),
            ApiResponse(responseCode = "404", description = "Event not found")
        ]
    )
    fun addTierToEvent(
        @PathVariable eventId: Long,
        @Valid @RequestBody request: CreateTicketTierRequest,
        authentication: Authentication
    ): ResponseEntity<TicketTierDTO> {
        val userEmail = authentication.name
        val isAdmin = isAdmin(authentication)

        val createdTier = ticketTierService.addTierToEvent(eventId, request, userEmail, isAdmin)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTier.toDTO())
    }

    @PutMapping("/{tierId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(
        summary = "Update ticket tier",
        description = "Updates an existing ticket tier. Only the organizer or admins can update a tier."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Ticket tier updated successfully"),
            ApiResponse(responseCode = "400", description = "Invalid update request"),
            ApiResponse(responseCode = "403", description = "Access denied"),
            ApiResponse(responseCode = "404", description = "Ticket tier not found")
        ]
    )
    fun updateTier(
        @PathVariable tierId: Long,
        @Valid @RequestBody request: UpdateTicketTierRequest,
        authentication: Authentication
    ): ResponseEntity<TicketTierDTO> {
        val userEmail = authentication.name
        val isAdmin = isAdmin(authentication)

        val updatedTier = ticketTierService.updateTier(tierId, request, userEmail, isAdmin)
        return ResponseEntity.ok(updatedTier.toDTO())
    }

    @DeleteMapping("/{tierId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(
        summary = "Delete ticket tier",
        description = "Deletes a ticket tier if no tickets have been sold."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Ticket tier deleted"),
            ApiResponse(responseCode = "400", description = "Tier cannot be deleted"),
            ApiResponse(responseCode = "403", description = "Access denied"),
            ApiResponse(responseCode = "404", description = "Ticket tier not found")
        ]
    )
    fun deleteTier(
        @PathVariable tierId: Long,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val userEmail = authentication.name
        val isAdmin = isAdmin(authentication)

        ticketTierService.deleteTier(tierId, userEmail, isAdmin)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{tierId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get ticket tier",
        description = "Fetch a ticket tier by its ID"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Ticket tier retrieved",
                content = [Content(schema = Schema(implementation = TicketTierDTO::class))]
            ),
            ApiResponse(responseCode = "404", description = "Ticket tier not found")
        ]
    )
    fun getTier(@PathVariable tierId: Long): ResponseEntity<TicketTierDTO> {
        val tier = ticketTierService.getTierById(tierId)
        return ResponseEntity.ok(tier)
    }

    private fun isAdmin(authentication: Authentication): Boolean {
        val adminRoles = setOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)

        return authentication.authorities.any { authority ->
            adminRoles.any { role ->
                authority.authority == "ROLE_$role"
            }
        }
    }
}