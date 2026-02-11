package com.thehiveproject.event.api.mapper
import com.thehiveproject.event.api.dto.UserDTO
import com.thehiveproject.event.api.utils.sanitizeForHtml
import com.thehiveproject.event.domain.user.User

fun User.toDTO(): UserDTO = UserDTO(
    id = id,
    username = sanitizeForHtml(username),
    email = sanitizeForHtml(email),
    roles = roles,
    createdBy = createdBy,
    updatedBy = updatedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    isActive = isActive,
    isDeleted = isDeleted
)
