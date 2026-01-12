package com.sam_the_dev.eventhive.domain.event

import com.sam_the_dev.eventhive.domain.user.User
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

data class Event(
    val id: Long?,
    val title: String,
    val description: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val location: String,
    val price: BigDecimal,
    val totalSeats: Int,
    val availableSeats: Int,
    val status: EventStatus,
    val organizerId: Long,
    val organizerName: String,
    val organizer: User,
    val createdBy: Long,
    val updatedBy: Long,
    val deletedBy: Long?,
    val isActive: Boolean,
    val isDeleted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)