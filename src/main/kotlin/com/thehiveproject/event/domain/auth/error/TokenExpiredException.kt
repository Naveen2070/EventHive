package com.thehiveproject.event.domain.auth.error

class TokenExpiredException(
    message: String = "JWT token has expired"
) : RuntimeException(message)