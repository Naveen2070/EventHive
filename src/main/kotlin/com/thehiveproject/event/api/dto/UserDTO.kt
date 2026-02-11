package com.thehiveproject.event.api.dto

import java.time.Instant

data class UserDTO(
    val id: Long?,
    val username: String,
    val email: String,
    val roles: Set<String>,
    val createdBy: Long,
    val updatedBy: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val isActive: Boolean,
    val isDeleted: Boolean
)