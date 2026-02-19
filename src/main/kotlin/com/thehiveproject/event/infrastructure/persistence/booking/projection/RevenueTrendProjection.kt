package com.thehiveproject.event.infrastructure.persistence.booking.projection

import java.time.LocalDate

interface RevenueTrendProjection {
    val date: LocalDate
    val revenue: Double
}
