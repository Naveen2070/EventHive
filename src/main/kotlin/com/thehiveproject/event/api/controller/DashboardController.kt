package com.thehiveproject.event.api.controller

import com.thehiveproject.event.api.dto.DashboardStatsDTO
import com.thehiveproject.event.domain.dashboard.DashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard")
@Tag(
    name = "Dashboard",
    description = "Dashboard endpoints for organizers"
)
class DashboardController(
    private val dashboardService: DashboardService
) {

    @Operation(
        summary = "Get organizer dashboard statistics",
        description = "Returns revenue, ticket sales, event counts, revenue trend and recent sales for the authenticated organizer.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Dashboard statistics retrieved successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DashboardStatsDTO::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized – invalid or missing JWT"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden – user is not an organizer"
            )
        ]
    )
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('events:ROLE_ORGANIZER')")
    fun getDashboardStats(
    ): ResponseEntity<DashboardStatsDTO> {

        val userId = com.thehiveproject.event.infrastructure.security.SecurityUtils.getCurrentUserId()
        val stats = dashboardService.getOrganizerStats(userId)

        return ResponseEntity.ok(stats)
    }
}
