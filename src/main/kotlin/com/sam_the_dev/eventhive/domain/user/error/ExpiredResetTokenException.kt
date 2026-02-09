package com.sam_the_dev.eventhive.domain.user.error

class ExpiredResetTokenException (
    message: String = "Reset token has expired"
) : RuntimeException(message)