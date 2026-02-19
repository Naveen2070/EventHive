package com.thehiveproject.event.domain.event.error

class UnauthorizedEventAccessException (
    message: String = "Access Denied: You are not the organizer of this event."
) : RuntimeException(message)