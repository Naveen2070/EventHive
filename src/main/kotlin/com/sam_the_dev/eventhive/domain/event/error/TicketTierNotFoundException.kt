package com.sam_the_dev.eventhive.domain.event.error

class TicketTierNotFoundException (
    message: String = "Ticket Tier not found"
):RuntimeException(message)