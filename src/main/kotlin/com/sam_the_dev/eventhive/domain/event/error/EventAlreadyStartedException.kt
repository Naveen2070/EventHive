package com.sam_the_dev.eventhive.domain.event.error

class EventAlreadyStartedException(
    eventId: Long,
    message: String ="Event $eventId has already started or is in the past"
) : RuntimeException(message)