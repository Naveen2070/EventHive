package com.sam_the_dev.eventhive.domain.event.error

class EventModificationNotAllowedException (
    message: String = "Event modification not allowed"
) : RuntimeException(message)