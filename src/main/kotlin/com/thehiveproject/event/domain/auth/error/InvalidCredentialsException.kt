package com.thehiveproject.event.domain.auth.error

class InvalidCredentialsException(
    message: String = "Invalid email or password"
) : RuntimeException(message)
