package com.thehiveproject.event.infrastructure.notification

import com.thehiveproject.event.domain.booking.event.BookingSuccessEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class BookingEventListener(
    private val notificationProducer: NotificationProducer
) {
    @Async
    @EventListener
    fun handleBookingSuccess(event: BookingSuccessEvent) {
        notificationProducer.sendBookingConfirmation(
            recipientEmail = event.userEmail,
            booking = event.booking
        )
    }
}
