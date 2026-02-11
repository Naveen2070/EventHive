package com.thehiveproject.event.api.controller

import com.thehiveproject.event.api.dto.RoleAssignmentRequest
import com.thehiveproject.event.api.dto.UserDTO
import com.thehiveproject.event.domain.role.RoleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
@Tag(
    name = "Admin",
    description = "APIs for admin operations"
)
class AdminController(
    private val roleService: RoleService
) {

    @Operation(
        summary = "Assign a role to a user",
        description = "Assigns a specific role to the user identified by userId. Returns the updated user."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Role assigned successfully",
                content = [Content(schema = Schema(implementation = UserDTO::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User or Role not found"
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request payload"
            )
        ]
    )
    @PutMapping("/roles/assign/{userId}")
    fun assignRole(
        @Parameter(description = "ID of the user to assign the role to", required = true)
        @PathVariable userId: Long,
        @Valid @RequestBody
        @Parameter(description = "Role assignment request payload", required = true)
        request: RoleAssignmentRequest
    ): ResponseEntity<UserDTO> {
        val updatedUser = roleService.assignRoleToUser(userId, request.roleName, request.updateBy)
        return ResponseEntity.ok(updatedUser)
    }

    @Operation(
        summary = "Remove a role from a user",
        description = "Removes a specific role from the user identified by userId. Returns the updated user."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Role removed successfully",
                content = [Content(schema = Schema(implementation = UserDTO::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User or Role not found"
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request payload"
            )
        ]
    )
    @DeleteMapping("/roles/remove/{userId}")
    fun removeRole(
        @Parameter(description = "ID of the user to remove the role from", required = true)
        @PathVariable userId: Long,
        @Valid @RequestBody
        @Parameter(description = "Role removal request payload", required = true)
        request: RoleAssignmentRequest
    ): ResponseEntity<UserDTO> {
        val updatedUser = roleService.removeRoleFromUser(userId, request.roleName, request.updateBy)
        return ResponseEntity.ok(updatedUser)
    }
}
