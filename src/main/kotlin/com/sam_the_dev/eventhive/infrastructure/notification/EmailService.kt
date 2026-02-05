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

    @Value($$"${frontend.url}")
    private lateinit var frontendUrl: String

    fun sendBookingConfirmation(to: String, booking: BookingDTO) {
        val subject = "Booking Confirmed: ${booking.eventTitle} 🎟️"

        val content = """
        Hello!
        
        Your booking for "${booking.eventTitle}" is confirmed 🎉
        
        --------------------------------------
        Booking Reference: ${booking.bookingReference}
        Tickets: ${booking.ticketsCount}
        Total Price: $${booking.totalPrice}
        Status: ${booking.status}
        --------------------------------------
        
        Please show this reference ID at the entrance.
        
        Thank you for using EventHive!
        — The EventHive Team
    """.trimIndent()

        sendEmail(to, subject, content)
    }


    fun sendPasswordResetLink(to: String, token: String, username: String) {
        val resetLink = "$frontendUrl/reset-password?token=$token"

        val subject = "Reset Your Password - EventHive 🔐"
        val text = """
            Hi $username,
            
            We received a request to reset your password for your EventHive account.
            
            Click the link below to set a new password:
            $resetLink
            
            This link will expire in 24 hours.
            
            If you didn't request this, you can safely ignore this email.
            
            - The EventHive Team
        """.trimIndent()

        sendEmail(to, subject, text)
    }

    fun sendPasswordChangedAlert(to: String, username: String) {
        val subject = "Security Alert: Password Changed 🛡️"
        val text = """
            Hi $username,
            
            Your password was successfully changed just now.
            
            If this was you, great! You can ignore this email.
            
            🚨 If you did NOT make this change, please contact support immediately to secure your account.
            
            - The EventHive Team
        """.trimIndent()

        sendEmail(to, subject, text)
    }

    // DRY (Don't Repeat Yourself) Helper
    private fun sendEmail(to: String, subject: String, content: String) {
        try {
            val message = SimpleMailMessage()
            message.from = senderEmail // "no-reply@eventhive.com"
            message.setTo(to)
            message.subject = subject
            message.text = content
            mailSender.send(message)
            logger.info("Email sent to $to | Subject: $subject")
        } catch (e: Exception) {
            logger.error("Failed to send email to $to", e)
        }
    }
}
