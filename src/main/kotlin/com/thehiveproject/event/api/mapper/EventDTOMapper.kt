package com.thehiveproject.event.api.mapper

import com.thehiveproject.event.api.dto.EventDTO
import com.thehiveproject.event.api.dto.TicketTierDTO
import com.thehiveproject.event.api.utils.sanitizeForHtml
import com.thehiveproject.event.domain.event.Event
import com.thehiveproject.event.domain.event.TicketTier
import java.math.BigDecimal

fun Event.toDTO(): EventDTO {
    val prices = ticketTiers.map { it.price }
    val minPrice = prices.minOrNull() ?: BigDecimal.ZERO
    val maxPrice = prices.maxOrNull() ?: BigDecimal.ZERO

    val priceRangeString = if (minPrice.compareTo(maxPrice) == 0) {
        if (minPrice.compareTo(BigDecimal.ZERO) == 0) "Free" else "$$minPrice"
    } else {
        "$$minPrice - $$maxPrice"
    }

    return EventDTO(
        id = id ?: 0,
        title = sanitizeForHtml(title),
        description = sanitizeForHtml(description),
        startDate = startDate,
        endDate = endDate,
        location = sanitizeForHtml(location),
        ticketTiers = ticketTiers.map {
            TicketTierDTO(
                it.id!!,
                it.name,
                it.price,
                it.totalAllocation,
                it.availableAllocation,
                it.validFrom,
                it.validUntil,
                it.createdBy,
                it.updatedBy
            )
        },
        priceRange = priceRangeString,
        status = status,
        organizerId = organizerId.toString(),
        organizerName = organizerName,
        createdAt = createdAt
    )
}

fun TicketTier.toDTO(): TicketTierDTO = TicketTierDTO(
    id = id!!,
    name = sanitizeForHtml(name),
    price = price,
    totalAllocation = totalAllocation,
    availableAllocation = availableAllocation,
    validFrom = validFrom,
    validUntil = validUntil,
    createdBy = createdBy,
    updatedBy = updatedBy,
)