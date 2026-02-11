package com.thehiveproject.event.infrastructure.persistence.booking

import com.thehiveproject.event.domain.booking.Booking
import com.thehiveproject.event.infrastructure.persistence.event.EventEntity
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierEntity
import com.thehiveproject.event.infrastructure.persistence.user.UserEntity
import java.time.ZoneId

fun BookingEntity.toDomain(): Booking =
    Booking(
        id = id,
        bookingReference = bookingReference,
        userId = user.id!!,
        eventId = event.id!!,
        ticketTierId = ticketTier.id!!,
        ticketTierName = ticketTier.name,
        eventTitle = event.title,
        eventDescription = event.description,
        eventDate = event.startDate.atZone(ZoneId.systemDefault()).toInstant(),
        eventEndDate = event.endDate.atZone(ZoneId.systemDefault()).toInstant(),
        eventLocation = event.location,
        ticketsCount = ticketsCount,
        totalPrice = totalPrice,
        status = status,
        createdBy = createdBy,
        updatedBy = updatedBy,
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant(),
        updatedAt = updatedAt.atZone(ZoneId.systemDefault()).toInstant(),
        isActive = true,
        isDeleted = false,
    )

fun Booking.toEntity(
    user: UserEntity,
    event: EventEntity,
    tier: TicketTierEntity
): BookingEntity =
    BookingEntity(
        id = id,
        bookingReference = bookingReference,
        user = user,
        event = event,
        ticketTier = tier,
        ticketsCount = ticketsCount,
        totalPrice = totalPrice,
        status = status,
        createdBy = createdBy,
        updatedBy = updatedBy,
    )