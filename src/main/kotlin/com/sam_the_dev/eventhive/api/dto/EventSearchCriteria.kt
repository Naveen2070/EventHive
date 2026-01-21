package com.sam_the_dev.eventhive.api.dto

import com.sam_the_dev.eventhive.domain.event.EventStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class EventSearchCriteria(
    val title: String? = null,
    val location: String? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val status: String? = EventStatus.PUBLISHED.name
)