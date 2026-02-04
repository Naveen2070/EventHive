package com.sam_the_dev.eventhive.domain.booking

import com.sam_the_dev.eventhive.api.utils.sanitizeForHtml
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class CheckInRequest(val bookingReference: String)

@Schema(description = "Response returned after a check-in attempt")
data class CheckInResponse(

    @field:Schema(
        description = "Indicates whether the check-in was successful",
        example = "true"
    )
    val success: Boolean,

    @field:Schema(
        description = "Human-readable message describing the result",
        example = "Check-in Successful"
    )
    val message: String,

    @field:Schema(
        description = "Username of the attendee",
        example = "john_doe"
    )
    val attendeeName: String? = null,

    @field:Schema(
        description = "Ticket category",
        example = "General"
    )
    val ticketType: String? = null,

    @field:Schema(
        description = "Date and time when the ticket was checked in",
        example = "2026-02-04T18:30:00"
    )
    val checkedInAt: LocalDateTime? = null
)

fun CheckInResponse.sanitizedForHtml(): CheckInResponse =
    CheckInResponse(
        success = success,
        message = sanitizeForHtml(message),
        attendeeName = sanitizeForHtml(attendeeName),
        ticketType = sanitizeForHtml(ticketType),
        checkedInAt = checkedInAt
    )