package com.sam_the_dev.eventhive.infrastructure.notification

import com.sam_the_dev.eventhive.api.dto.BookingDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    @Value("\${spring.mail.username}")
    private lateinit var senderEmail: String

    fun sendBookingConfirmation(to: String, booking: BookingDTO) {
        try {
            val message = SimpleMailMessage()
            message.from = "no-reply@eventhive.com"
            message.setTo(to)
            message.subject = "Booking Confirmed: ${booking.eventTitle} 🎟️"
            message.text = """
                Hello!
                
                Your booking for "${booking.eventTitle}" is confirmed!
                
                --------------------------------------
                Booking Reference: ${booking.bookingReference}
                Tickets: ${booking.ticketsCount}
                Total Price: $${booking.totalPrice}
                Status: ${booking.status}
                --------------------------------------
                
                Please show this reference ID at the entrance.
                
                Thank you for using EventHive!
            """.trimIndent()

            mailSender.send(message)
            logger.info("Confirmation email sent to $to for booking ${booking.bookingReference}")

        } catch (e: Exception) {
            // Note: In production, you might want to retry this or log it to an alert system
            logger.error("Failed to send email to $to", e)
        }
    }
}
