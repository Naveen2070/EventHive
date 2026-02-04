package com.sam_the_dev.eventhive.domain.booking

import com.sam_the_dev.eventhive.domain.event.Event
import com.sam_the_dev.eventhive.domain.user.User
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.util.*

class Booking(
    val id: Long? = null,
    val bookingReference: String,
    val userId: Long,
    val eventId: Long,
    val eventTitle: String,
    val eventDescription: String,
    val eventDate: Instant,
    val eventEndDate: Instant,
    val eventLocation: String,
    val ticketsCount: Int,
    val totalPrice: BigDecimal,
    val status: BookingStatus,
    val createdBy: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isActive: Boolean,
    val isDeleted: Boolean
){
    companion object {
        fun create(
            user: User,
            event: Event,
            ticketsCount: Int,
            pricePerTicket: BigDecimal,
            createdBy: Long
        ): Booking {
            return Booking(
                bookingReference = UUID.randomUUID().toString(),
                userId = user.id!!,
                eventId = event.id!!,
                eventTitle = event.title,
                eventDescription = event.description,
                eventEndDate = event.endDate.atZone(ZoneId.systemDefault()).toInstant(),
                eventDate = event.startDate.atZone(ZoneId.systemDefault()).toInstant(),
                eventLocation = event.location,
                ticketsCount = ticketsCount,
                totalPrice = pricePerTicket * BigDecimal(ticketsCount),
                status = BookingStatus.PENDING_PAYMENT,
                createdBy = createdBy,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                isActive = true,
                isDeleted = false
            )
        }
    }

}

