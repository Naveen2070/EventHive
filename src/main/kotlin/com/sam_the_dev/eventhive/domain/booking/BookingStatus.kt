package com.sam_the_dev.eventhive.domain.booking

enum class BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    REFUNDED,
    EXPIRED,
    USED,
}