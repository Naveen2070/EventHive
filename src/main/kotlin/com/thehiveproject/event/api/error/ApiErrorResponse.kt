package com.thehiveproject.event.api.error

import java.time.LocalDateTime

data class ApiErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)