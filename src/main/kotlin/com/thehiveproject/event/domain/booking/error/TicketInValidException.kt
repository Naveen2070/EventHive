package com.thehiveproject.event.domain.booking.error

class TicketInValidException (
    message:String ="The ticket is invalid"
): RuntimeException(message)