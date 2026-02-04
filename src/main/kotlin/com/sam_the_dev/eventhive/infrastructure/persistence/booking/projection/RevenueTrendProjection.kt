package com.sam_the_dev.eventhive.infrastructure.persistence.booking.projection

import java.time.LocalDate

interface RevenueTrendProjection {
    val date: LocalDate
    val revenue: Double
}
