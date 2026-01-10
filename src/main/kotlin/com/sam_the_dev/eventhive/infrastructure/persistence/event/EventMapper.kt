package com.sam_the_dev.eventhive.infrastructure.persistence.event

import com.sam_the_dev.eventhive.domain.event.Event
import com.sam_the_dev.eventhive.infrastructure.persistence.user.toDomain

fun EventEntity.toDomain(): Event = Event(
    id = id ?: 0L,
    title = title,
    description = description,
    startDate = startDate,
    endDate = endDate,
    location = location,
    price = price,
    totalSeats = totalSeats,
    availableSeats = availableSeats,
    status = status,
    organizerId = organizer.id ?: 0L,
    organizerName = organizer.username,
    organizer = organizer.toDomain(),
    createdBy = createdBy,
    updatedBy = updatedBy,
    deletedBy = deletedBy,
    isActive = isActive,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)