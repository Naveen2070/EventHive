package com.sam_the_dev.eventhive.api.dto

import com.sam_the_dev.eventhive.domain.booking.BookingStatus
import java.math.BigDecimal
import java.time.Instant

data class BookingDTO(
    val bookingId: Long,
    val bookingReference: String,
    val eventTitle: String,
    val ticketsCount: Int,
    val totalPrice: BigDecimal,
    val status: BookingStatus,
    val bookedAt: Instant
)
data class CreateBookingRequest(
    val eventId: Long,
    val ticketsCount: Int
)

data class UpdateBookingStatusRequest(
    val status: BookingStatus
)

data class PaymentWebhookPayload(
    val bookingReference: String,
    val paymentId: String,
    val status: String
)