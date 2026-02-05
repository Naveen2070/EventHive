package com.sam_the_dev.eventhive.infrastructure.persistence.event

import com.sam_the_dev.eventhive.domain.event.Event
import com.sam_the_dev.eventhive.domain.event.TicketTier
import com.sam_the_dev.eventhive.infrastructure.persistence.user.toDomain

fun TicketTierEntity.toDomain(): TicketTier = TicketTier(
    id = id,
    name = name,
    price = price,
    totalAllocation = totalAllocation,
    availableAllocation = availableAllocation,
    validFrom = validFrom,
    validUntil = validUntil
)

fun EventEntity.toDomain(): Event = Event(
    id = id ?: 0L,
    title = title,
    description = description,
    startDate = startDate,
    endDate = endDate,
    location = location,
    ticketTiers = ticketTiers.map { it.toDomain() },
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