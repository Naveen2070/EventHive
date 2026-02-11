package com.thehiveproject.event.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateUserRequest(
    @field:Size(min = 3, message = "Username must be at least 3 characters")
    val username: String?
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Old password is required")
    val oldPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val newPassword: String
)

data class ForgotPasswordRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "Token is required")
    val token: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val newPassword: String
)