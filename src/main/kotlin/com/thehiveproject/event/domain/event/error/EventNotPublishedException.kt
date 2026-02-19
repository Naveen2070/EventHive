package com.thehiveproject.event.domain.event.error

class EventNotPublishedException(
    eventName: String,
    message:String = "Event $eventName is not published",
) : RuntimeException(message)