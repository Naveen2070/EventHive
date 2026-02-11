package com.thehiveproject.event.api.controller

import com.thehiveproject.event.api.dto.*
import com.thehiveproject.event.api.error.ApiErrorResponse
import com.thehiveproject.event.domain.user.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
@Tag(
    name = "Users",
    description = "APIs for managing user profiles, credentials, and password recovery"
)
class UserController(
    private val userService: UserService
) {

    @Operation(
        summary = "Get user by ID",
        description = "Retrieves user details using the user ID. Requires authentication.",
        security = [SecurityRequirement(name = "bearer-key")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User retrieved successfully",
                content = [Content(schema = Schema(implementation = UserDTO::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/users/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getUser(
        @Parameter(description = "User ID", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<UserDTO> =
        ResponseEntity.ok(userService.getUserById(id))


    @Operation(
        summary = "Update user profile",
        description = "Allows an authenticated user to update their own profile information.",
        security = [SecurityRequirement(name = "bearer-key")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User updated successfully",
                content = [Content(schema = Schema(implementation = UserDTO::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validation failed",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Access denied",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Username already exists",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            )
        ]
    )
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserRequest,
        authentication: Authentication
    ): ResponseEntity<UserDTO> {
        val currentUserEmail = authentication.name
        return ResponseEntity.ok(userService.updateUser(id, request, currentUserEmail))
    }


    @Operation(
        summary = "Change password",
        description = "Allows an authenticated user to change their own password.",
        security = [SecurityRequirement(name = "bearer-key")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Password updated successfully"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid old password",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Access denied",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/change-password/{id}")
    @PreAuthorize("isAuthenticated()")
    fun changePassword(
        @PathVariable id: Long,
        @Valid @RequestBody request: ChangePasswordRequest,
        authentication: Authentication
    ): ResponseEntity<String> {
        userService.changePassword(id, request, authentication.name)
        return ResponseEntity.ok("Password updated successfully")
    }


    @Operation(
        summary = "Forgot password",
        description = "Initiates the password reset process by sending a reset link to the user's email."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Password reset email sent"),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/forgot-password")
    fun forgotPassword(
        @Valid @RequestBody request: ForgotPasswordRequest
    ): ResponseEntity<String> {
        userService.initiatePasswordReset(request.email)
        return ResponseEntity.ok("A reset link has been sent to your email.")
    }


    @Operation(
        summary = "Reset password",
        description = "Completes password reset using a valid reset token."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Password reset successfully"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid or expired reset token",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/reset-password")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<String> {
        userService.completePasswordReset(request.token, request.newPassword)
        return ResponseEntity.ok("Password has been reset successfully. You can now login.")
    }
}