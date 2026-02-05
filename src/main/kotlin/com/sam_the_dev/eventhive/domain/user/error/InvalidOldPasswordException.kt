package com.sam_the_dev.eventhive.domain.user.error

class InvalidOldPasswordException (
    message: String = "Invalid old password"
) : RuntimeException(message)