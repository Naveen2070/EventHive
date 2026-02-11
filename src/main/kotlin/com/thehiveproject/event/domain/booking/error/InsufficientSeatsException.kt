package com.thehiveproject.event.domain.booking.error

class InsufficientSeatsException(
    requested: Int,
    available: Int,
    message: String = "Not enough seats available. Requested: $requested, Available: $available"
) : RuntimeException(message)
