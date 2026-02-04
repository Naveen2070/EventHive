package com.sam_the_dev.eventhive.api.mapper

import com.sam_the_dev.eventhive.api.dto.EventDTO
import com.sam_the_dev.eventhive.api.utils.sanitizeForHtml
import com.sam_the_dev.eventhive.domain.event.Event

fun Event.toDTO(): EventDTO = EventDTO(
    id = id ?: 0,
    title = sanitizeForHtml(title),
    description = sanitizeForHtml(description),
    startDate = startDate,
    endDate = endDate,
    location = sanitizeForHtml(location),
    price = price,
    availableSeats = availableSeats,
    status = status,
    organizerName = sanitizeForHtml(organizerName),
    totalSeats = totalSeats,
    organizerId = organizerId,
    createdAt = createdAt
)