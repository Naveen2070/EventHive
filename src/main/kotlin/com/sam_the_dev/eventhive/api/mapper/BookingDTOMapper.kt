package com.sam_the_dev.eventhive.api.mapper

import com.sam_the_dev.eventhive.api.dto.BookingDTO
import com.sam_the_dev.eventhive.domain.booking.Booking

fun Booking.toDTO(): BookingDTO =
    BookingDTO(
        bookingId = requireNotNull(id) { "Booking ID cannot be null when converting to DTO" },
        bookingReference = bookingReference,
        eventTitle = eventTitle,
        ticketsCount = ticketsCount,
        totalPrice = totalPrice,
        status = status,
        bookedAt = createdAt,
        eventDescription = eventDescription,
        eventDate = eventDate,
        eventLocation = eventLocation
    )