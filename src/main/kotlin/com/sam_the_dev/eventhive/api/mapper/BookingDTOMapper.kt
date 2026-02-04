package com.sam_the_dev.eventhive.api.mapper

import com.sam_the_dev.eventhive.api.dto.BookingDTO
import com.sam_the_dev.eventhive.api.utils.sanitizeForHtml
import com.sam_the_dev.eventhive.domain.booking.Booking

fun Booking.toDTO(): BookingDTO =
    BookingDTO(
        bookingId = requireNotNull(id) { "Booking ID cannot be null when converting to DTO" },
        bookingReference = bookingReference,
        eventId = eventId,
        eventTitle = sanitizeForHtml(eventTitle),
        eventDescription = sanitizeForHtml(eventDescription),
        eventDate = eventDate,
        eventEndDate = eventEndDate,
        eventLocation = sanitizeForHtml(eventLocation),
        ticketsCount = ticketsCount,
        totalPrice = totalPrice,
        status = status,
        bookedAt = createdAt
    )
