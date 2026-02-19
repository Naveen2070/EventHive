package com.thehiveproject.event.domain.booking.error

class TicketAlreadyUsedException (
    message: String = "Ticket has already been used."
): RuntimeException(message)