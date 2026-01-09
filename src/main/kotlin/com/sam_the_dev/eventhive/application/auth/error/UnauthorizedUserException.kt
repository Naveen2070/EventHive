package com.sam_the_dev.eventhive.application.auth.error

class UnauthorizedUserException (
    message: String = "Unauthorized User"
) : RuntimeException(message)