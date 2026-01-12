package com.sam_the_dev.eventhive.domain.event.error

class EventNotPublishedException(
    eventId: Long,
    message:String = "Event $eventId is not published",
) : RuntimeException(message)