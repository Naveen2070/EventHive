package com.sam_the_dev.eventhive.application.auth.error

class TokenExpiredException(
    message: String = "JWT token has expired"
) : RuntimeException(message)