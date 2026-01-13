package com.sam_the_dev.eventhive.domain.event.error

class UnauthorizedEventAccessException (
    message: String = "Access Denied: You are not the organizer of this event."
) : RuntimeException(message)