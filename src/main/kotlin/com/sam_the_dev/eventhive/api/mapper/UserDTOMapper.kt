package com.sam_the_dev.eventhive.api.mapper
import com.sam_the_dev.eventhive.api.dto.UserDTO
import com.sam_the_dev.eventhive.api.utils.sanitizeForHtml
import com.sam_the_dev.eventhive.domain.user.User

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
