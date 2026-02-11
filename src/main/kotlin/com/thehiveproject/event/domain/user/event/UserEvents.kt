package com.thehiveproject.event.domain.user.event

data class PasswordResetInitiatedEvent(
    val email: String,
    val token: String,
    val username: String
)

data class PasswordChangedEvent(
    val email: String,
    val username: String
)