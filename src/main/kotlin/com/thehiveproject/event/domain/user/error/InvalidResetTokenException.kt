package com.thehiveproject.event.domain.user.error

class InvalidResetTokenException(
    message: String = "Invalid reset token"
) : RuntimeException(message)