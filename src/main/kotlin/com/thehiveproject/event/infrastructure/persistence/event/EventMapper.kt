package com.thehiveproject.event.infrastructure.persistence.event

import com.thehiveproject.event.api.dto.UserSummaryDTO
import com.thehiveproject.event.domain.event.Event
import com.thehiveproject.event.domain.event.TicketTier

fun TicketTierEntity.toDomain(): TicketTier = TicketTier(
    id = this.id,
    name = this.name,
    price =this.price,
    totalAllocation = this.totalAllocation,
    availableAllocation = this.availableAllocation,
    validFrom = this.validFrom,
    validUntil = this.validUntil,
    createdBy = this.createdBy,
    updatedBy =this.updatedBy,
)
fun EventEntity.toDomain(userData: UserSummaryDTO): Event = Event(
    id = id ?: 0L,
    title = title,
    description = description,
    startDate = startDate,
    endDate = endDate,
    location = location,
    ticketTiers = ticketTiers.map { it.toDomain() },
    status = status,
    organizerId = organizerId,
    organizerName = userData.fullName,
    createdBy = createdBy,
    updatedBy = updatedBy,
    deletedBy = deletedBy,
    isActive = isActive,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)