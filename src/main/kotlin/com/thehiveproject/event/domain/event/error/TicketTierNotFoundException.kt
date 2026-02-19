package com.thehiveproject.event.domain.event.error

class TicketTierNotFoundException (
    message: String = "Ticket Tier not found"
):RuntimeException(message)