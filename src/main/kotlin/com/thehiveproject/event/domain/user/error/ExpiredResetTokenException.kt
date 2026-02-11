package com.thehiveproject.event.domain.user.error

class ExpiredResetTokenException (
    message: String = "Reset token has expired"
) : RuntimeException(message)