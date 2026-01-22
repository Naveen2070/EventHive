package com.sam_the_dev.eventhive.infrastructure.notification

import com.sam_the_dev.eventhive.domain.booking.event.BookingSuccessEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class BookingEventListener(
    private val emailService: EmailService
) {
    @Async
    @EventListener
    fun handleBookingSuccess(event: BookingSuccessEvent) {
        // Simulate a delay to prove it doesn't block the API
        // Thread.sleep(5000)
        emailService.sendBookingConfirmation(
            to = event.userEmail,
            booking = event.booking
        )
    }
}
