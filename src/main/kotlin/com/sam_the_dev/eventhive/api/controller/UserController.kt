package com.sam_the_dev.eventhive.api.controller

import com.sam_the_dev.eventhive.api.dto.UserDTO
import com.sam_the_dev.eventhive.domain.user.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
@Tag(
    name = "Users",
    description = "APIs for retrieving user information"
)
class UserController(
    private val userService: UserService
) {

    @Operation(
        summary = "Get user by ID",
        description = "Retrieves user details by user ID."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User retrieved successfully",
                content = [Content(schema = Schema(implementation = UserDTO::class))]
            ),
            ApiResponse(responseCode = "404", description = "User not found"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @GetMapping("/users/{id}")
    fun getUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable id: Long
    ): ResponseEntity<UserDTO> {
        val user = userService.getUserById(id)
        return ResponseEntity.ok(user)
    }
}
