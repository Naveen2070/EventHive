package com.thehiveproject.event.domain.auth.error

class UnauthorizedUserException (
    message: String = "Unauthorized User"
) : RuntimeException(message)