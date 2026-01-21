package com.sam_the_dev.eventhive.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "Username or email is required")
    val identifier: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String
)

data class AuthResponse(
    val token: String,
    val identifier: String
)