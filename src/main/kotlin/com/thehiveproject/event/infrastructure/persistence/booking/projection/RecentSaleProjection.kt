package com.thehiveproject.event.infrastructure.persistence.booking.projection

import java.time.LocalDateTime

interface RecentSaleProjection {
    val id: Long
    val eventName: String
    val tierName: String
    val customerName: String
    val tickets: Int
    val amount: Double
    val date: LocalDateTime
}
