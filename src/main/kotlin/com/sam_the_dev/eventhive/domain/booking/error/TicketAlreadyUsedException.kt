package com.sam_the_dev.eventhive.domain.booking.error

class TicketAlreadyUsedException (
    message: String = "Ticket has already been used."
): RuntimeException(message)