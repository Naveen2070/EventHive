package com.thehiveproject.event.api.dto

import com.thehiveproject.event.domain.event.EventStatus
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

data class TicketTierDTO(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val totalAllocation: Int,
    val availableAllocation: Int,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val createdBy: Long,
    val updatedBy: Long,
)

data class CreateTicketTierRequest(
    @field:NotBlank(message = "Tier name is required")
    val name: String,

    @field:NotNull(message = "Price is required")
    @field:Min(value = 0, message = "Price cannot be negative")
    var price: BigDecimal,

    @field:Min(value = 1, message = "Total allocation must be at least 1")
    var totalAllocation: Int,

    @field:NotNull(message = "Valid From date is required")
    var validFrom: LocalDateTime,

    @field:NotNull(message = "Valid Until date is required")
    var validUntil: LocalDateTime,

    @field:NotNull(message = "Valid createdBy is required")
    var createdBy: Long,
)

data class UpdateTicketTierRequest(
    @field:Size(min = 1, message = "Name must have at least 1 character")
    val name: String?,

    @field:Min(value = 0, message = "Price cannot be negative")
    val price: BigDecimal?,

    @field:Min(value = 1, message = "Total allocation must be at least 1")
    val totalAllocation: Int?,

    val validFrom: LocalDateTime?,
    val validUntil: LocalDateTime?,

    @field:NotNull(message = "Valid updatedBy is required")
    var updatedBy: Long,
)

data class EventDTO(
    val id: Long,
    val title: String,
    val description: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val location: String,
    val ticketTiers: List<TicketTierDTO>,
    val priceRange: String,
    val status: EventStatus,
    val organizerId: Long,
    val organizerName: String,
    val createdAt: Instant
)

data class CreateEventRequest(
    @field:NotBlank(message = "Title is required")
    val title: String,

    @field:NotBlank(message = "Description is required")
    val description: String,

    @field:NotNull(message = "Start date is required")
    var startDate: LocalDateTime,

    @field:NotNull(message = "End date is required")
    var endDate: LocalDateTime,

    @field:NotBlank(message = "Location is required")
    val location: String,

    @field:NotEmpty(message = "At least one ticket tier is required")
    val ticketTiers: List<CreateTicketTierRequest>,

    val organizerEmail: String,
    var createdBy: Long
)

data class UpdateEventRequest(
    val title: String?,
    val description: String?,
    val location: String?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?
    // Note: We removed price/seats.
    // Complex Tier updates (adding/removing) should usually be separate endpoints,
)

data class ChangeEventStatusRequest(
    val status: EventStatus
)