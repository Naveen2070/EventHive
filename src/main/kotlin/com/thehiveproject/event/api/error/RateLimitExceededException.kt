package com.thehiveproject.event.api.error

class RateLimitExceededException(
    message: String = "Too many requests. Please try again later."
) : RuntimeException(message)