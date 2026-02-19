package com.thehiveproject.event.domain.booking

enum class BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    REFUNDED,
    EXPIRED,
    CHECKED_IN,
}