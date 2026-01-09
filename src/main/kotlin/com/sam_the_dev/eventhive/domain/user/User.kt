package com.sam_the_dev.eventhive.domain.user

import java.time.Instant

data class User(
    val id: Long?,
    val username: String,
    val email: String,
    val password: String,
    val createdBy: Long,
    val updatedBy: Long,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false
)