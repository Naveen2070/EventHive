package com.sam_the_dev.eventhive.infrastructure.persistence.user

import com.sam_the_dev.eventhive.domain.user.User

fun UserEntity.toDomain(): User = User(
    id = id,
    username = username,
    email = email,
    password = password,
    createdBy = createdBy,
    updatedBy = updatedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    isActive = isActive,
    isDeleted = isDeleted
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    username = username,
    email = email,
    password = password,
    createdBy = createdBy,
    updatedBy = updatedBy,
)