package com.thehiveproject.event.domain.booking.error

class ResourceAccessDeniedException(
    message: String = "You can only modify resources you have access to"
) : RuntimeException(message)