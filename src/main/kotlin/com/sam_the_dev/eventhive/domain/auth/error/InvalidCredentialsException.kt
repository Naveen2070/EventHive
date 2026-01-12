package com.sam_the_dev.eventhive.domain.auth.error

class InvalidCredentialsException(
    message: String = "Invalid email or password"
) : RuntimeException(message)
