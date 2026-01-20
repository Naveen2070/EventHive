package com.sam_the_dev.eventhive.domain.booking.error

class BookingNotFoundException (
    message: String = "Booking not found"
): RuntimeException(message)