package com.sam_the_dev.eventhive.api.dto

data class LoginRequest(
    val identifier: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val identifier: String
)