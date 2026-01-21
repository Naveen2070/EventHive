package com.sam_the_dev.eventhive.api.dto

import com.sam_the_dev.eventhive.domain.event.EventStatus
import jakarta.validation.constraints.*
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
    @field:NotBlank(message = "Title is required")
    @field:Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    val title: String,

    @field:NotBlank(message = "Description is required")
    @field:Size(min = 1, max = 2000, message = "Description must be between 1 and 2000 characters")
    val description: String,

    @field:NotNull(message = "Start date is required")
    @field:Future(message = "Start date must be in the future")
    var startDate: LocalDateTime,

    @field:NotNull(message = "End date is required")
    @field:Future(message = "End date must be in the future")
    var endDate: LocalDateTime,

    @field:NotBlank(message = "Location is required")
    val location: String,

    @field:NotNull(message = "Price is required")
    @field:Min(value = 0, message = "Price cannot be negative")
    var price: BigDecimal,

    @field:Min(value = 1, message = "Total seats must be at least 1")
    val totalSeats: Int,

    @field:NotBlank(message = "Organizer email is required")
    @field:Email(message = "Invalid email format")
    val organizerEmail: String,

    @field:NotNull(message = "Created by user ID is required")
    @field:Positive(message = "Created by user ID must be positive")
    var createdBy: Long
)

data class UpdateEventRequest(
    @field:Size(min = 3, message = "Title must be at least 3 characters")
    val title: String?,

    val description: String?,
    val location: String?,

    @field:Min(value = 0, message = "Price cannot be negative")
    val price: BigDecimal?,

    @field:Min(value = 1, message = "Total seats must be at least 1")
    val totalSeats: Int?,

    @field:Future(message = "Start date must be in the future")
    val startDate: LocalDateTime?,

    @field:Future(message = "End date must be in the future")
    val endDate: LocalDateTime?
)

data class ChangeEventStatusRequest(
    @field:NotNull(message = "Event status is required")
    var status: EventStatus
)
