package com.sam_the_dev.eventhive.domain.booking.error

class TicketInValidException (
    message:String ="The ticket is invalid"
): RuntimeException(message)