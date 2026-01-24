package com.sam_the_dev.eventhive.api.error

class RateLimitExceededException(
    message: String = "Too many requests. Please try again later."
) : RuntimeException(message)