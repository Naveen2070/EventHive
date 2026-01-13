package com.sam_the_dev.eventhive.domain.event.error

class EventAlreadyStartedException(
    eventTitle: String,
    message: String ="Event $eventTitle has already started or is in the past"
) : RuntimeException(message)