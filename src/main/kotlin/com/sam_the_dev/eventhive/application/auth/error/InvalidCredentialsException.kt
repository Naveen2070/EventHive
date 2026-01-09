package com.sam_the_dev.eventhive.application.auth.error

class InvalidCredentialsException(
    message: String = "Invalid email or password"
) : RuntimeException(message)
