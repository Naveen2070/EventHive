package com.thehiveproject.event.domain.user.error

class InvalidOldPasswordException (
    message: String = "Invalid old password"
) : RuntimeException(message)