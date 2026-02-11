package com.thehiveproject.event.api.mapper

import com.thehiveproject.event.api.dto.BookingDTO
import com.thehiveproject.event.api.utils.sanitizeForHtml
import com.thehiveproject.event.domain.booking.Booking

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
        ticketTierName = ticketTierName,
        ticketsCount = ticketsCount,
        totalPrice = totalPrice,
        status = status,
        bookedAt = createdAt
    )
