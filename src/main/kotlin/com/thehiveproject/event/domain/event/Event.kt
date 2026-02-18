package com.thehiveproject.event.domain.event

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

data class TicketTier(
    val id: Long?,
    val name: String,
    val price: BigDecimal,
    val totalAllocation: Int,
    val availableAllocation: Int,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val createdBy: Long,
    val updatedBy: Long,
)

data class Event(
    val id: Long?,
    val title: String,
    val description: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val location: String,
    val ticketTiers: List<TicketTier>,
    val status: EventStatus,
    val organizerId: Long,
    val organizerName: String,
    val createdBy: Long,
    val updatedBy: Long,
    val deletedBy: Long?,
    val isActive: Boolean,
    val isDeleted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)