package com.thehiveproject.event.domain.booking.error

class BookingNotFoundException (
    message: String = "Booking not found"
): RuntimeException(message)