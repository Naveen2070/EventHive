package com.sam_the_dev.eventhive.domain.event.error

class EventNotPublishedException(
    eventName: String,
    message:String = "Event $eventName is not published",
) : RuntimeException(message)