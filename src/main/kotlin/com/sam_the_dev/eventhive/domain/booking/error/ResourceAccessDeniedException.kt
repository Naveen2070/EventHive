package com.sam_the_dev.eventhive.domain.booking.error

class ResourceAccessDeniedException(
    message: String = "You can only modify resources you have access to"
) : RuntimeException(message)