package com.sam_the_dev.eventhive.api.mapper

import com.sam_the_dev.eventhive.api.dto.EventDTO
import com.sam_the_dev.eventhive.domain.event.Event

fun Event.toDTO(): EventDTO = EventDTO(
    id = id,
    title = title,
    description = description,
    startDate = startDate,
    endDate = endDate,
    location = location,
    price = price,
    availableSeats = availableSeats,
    status = status,
    organizerName = organizerName,
    totalSeats = totalSeats,
    organizerId = organizerId,
    createdAt = createdAt
)