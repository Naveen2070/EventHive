package com.thehiveproject.event.api.dto

import com.thehiveproject.event.domain.event.EventStatus
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