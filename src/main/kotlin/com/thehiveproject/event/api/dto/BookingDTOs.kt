package com.thehiveproject.event.api.dto

import com.thehiveproject.event.api.utils.sanitizeForHtml
import com.thehiveproject.event.domain.booking.BookingStatus
import com.thehiveproject.event.domain.booking.CheckInStatus
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

data class BookingDTO(
    val bookingId: Long,
    val bookingReference: String,
    val eventId: Long,
    val eventTitle: String,
    val ticketTierName: String,
    val eventDescription: String,
    val eventDate: Instant,
    val eventEndDate: Instant,
    val eventLocation: String,
    val ticketsCount: Int,
    val totalPrice: BigDecimal,
    val status: BookingStatus,
    val bookedAt: Instant
)

data class CreateBookingRequest(
    @field:NotNull(message = "Event ID is required")
    @field:Positive(message = "Event ID must be positive")
    var eventId: Long,

    @field:NotNull(message = "Ticket Tier ID is required")
    var ticketTierId: Long,

    @field:NotNull(message = "Tickets count is required")
    @field:Min(value = 1, message = "At least 1 ticket must be booked")
    @field:Max(value = 10, message = "You cannot book more than 10 tickets at once")
    var ticketsCount: Int
)

data class UpdateBookingStatusRequest(
    @field:NotNull(message = "Booking status is required")
    var status: BookingStatus
)

data class PaymentWebhookPayload(
    @field:NotBlank(message = "Booking reference is required")
    val bookingReference: String,

    @field:NotBlank(message = "Payment ID is required")
    val paymentId: String,

    @field:NotBlank(message = "Payment status is required")
    val status: String
)

data class CheckInRequest(
    val bookingReference: String
)

data class CheckInResponse(
    val success: Boolean,
    val status: CheckInStatus,
    val message: String,
    val attendeeName: String? = null,
    val ticketTierName: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

fun CheckInResponse.toSanitized(): CheckInResponse =
    CheckInResponse(
        success = success,
        status = status,
        message = sanitizeForHtml(message),
        attendeeName = sanitizeForHtml(attendeeName),
        ticketTierName = sanitizeForHtml(ticketTierName),
        timestamp = timestamp
    )
