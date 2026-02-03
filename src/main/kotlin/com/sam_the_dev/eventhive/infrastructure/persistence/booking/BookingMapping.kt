package com.sam_the_dev.eventhive.infrastructure.persistence.booking

import com.sam_the_dev.eventhive.domain.booking.Booking
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserEntity
import java.time.ZoneId

fun BookingEntity.toDomain(): Booking =
    Booking(
        id = id,
        bookingReference = bookingReference,
        userId = user.id!!,
        eventId = event.id!!,
        eventTitle = event.title,
        ticketsCount = ticketsCount,
        totalPrice = totalPrice,
        status = status,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isActive = isActive,
        isDeleted = isDeleted,
        eventDescription = event.description,
        eventDate = event.startDate.atZone(ZoneId.systemDefault()).toInstant(),
        eventLocation =event.location
    )

fun Booking.toEntity(
    user: UserEntity,
    event: EventEntity
): BookingEntity =
    BookingEntity(
        id = id,
        bookingReference = bookingReference,
        user = user,
        event = event,
        ticketsCount = ticketsCount,
        totalPrice = totalPrice,
        status = status,
        createdBy = createdBy,
        updatedBy = createdBy
    )
