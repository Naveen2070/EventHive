package com.thehiveproject.event.infrastructure.notification

import com.thehiveproject.event.api.dto.BookingDTO
import com.thehiveproject.event.configuration.RabbitMQConfig
import com.thehiveproject.event.infrastructure.notification.dto.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class NotificationProducer(
    private val rabbitTemplate: RabbitTemplate
) {

    private val logger = LoggerFactory.getLogger(NotificationProducer::class.java)

    fun sendBookingConfirmation(recipientEmail: String, booking: BookingDTO) {
        val variables = mapOf(
            "bookingReference" to booking.bookingReference,
            "eventTitle" to booking.eventTitle,
            "ticketsCount" to booking.ticketsCount.toString(),
            "totalPrice" to booking.totalPrice.toString()
        )

        val event = NotificationEvent(
            recipientEmail = recipientEmail,
            subject = "Booking Confirmed: ${booking.eventTitle}",
            templateCode = "BOOKING_CONFIRMED",
            variables = variables
        )

        logger.info("Sending 'core.email' to Exchange: ${RabbitMQConfig.EXCHANGE_NAME}")

        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_CORE_EMAIL,
                event
            )
        } catch (e: Exception) {
            logger.error("Failed to publish booking notification", e)
        }
    }
}