package com.sam_the_dev.eventhive.api.mapper

import com.sam_the_dev.eventhive.api.dto.EventDTO
import com.sam_the_dev.eventhive.api.dto.TicketTierDTO
import com.sam_the_dev.eventhive.api.utils.sanitizeForHtml
import com.sam_the_dev.eventhive.domain.event.Event
import com.sam_the_dev.eventhive.domain.event.TicketTier
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
                it.validUntil
            )
        },
        priceRange = priceRangeString,
        status = status,
        organizerName = sanitizeForHtml(organizerName),
        organizerId = organizerId,
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
    validUntil = validUntil
)