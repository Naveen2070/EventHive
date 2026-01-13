package com.sam_the_dev.eventhive.api.dto

import com.sam_the_dev.eventhive.domain.event.EventStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

data class EventDTO(
    val id: Long,
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
    val createdAt: Instant
)

data class CreateEventRequest(
    val title: String,
    val description: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val location: String,
    val price: BigDecimal,
    val totalSeats: Int,
    val organizerEmail: String,
    val createdBy: Long,
)

data class UpdateEventRequest(
    val title: String?,
    val description: String?,
    val location: String?,
    val price: BigDecimal?,
    val totalSeats: Int?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?
)

data class ChangeEventStatusRequest(
    val status: EventStatus
)
