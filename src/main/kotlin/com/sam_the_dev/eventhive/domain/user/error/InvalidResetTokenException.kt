package com.sam_the_dev.eventhive.domain.user.error

class InvalidResetTokenException(
    message: String = "Invalid reset token"
) : RuntimeException(message)