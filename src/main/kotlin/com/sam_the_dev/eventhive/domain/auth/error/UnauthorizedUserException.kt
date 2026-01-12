package com.sam_the_dev.eventhive.domain.auth.error

class UnauthorizedUserException (
    message: String = "Unauthorized User"
) : RuntimeException(message)