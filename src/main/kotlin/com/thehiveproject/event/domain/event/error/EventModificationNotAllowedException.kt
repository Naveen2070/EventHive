package com.thehiveproject.event.domain.event.error

class EventModificationNotAllowedException (
    message: String = "Event modification not allowed"
) : RuntimeException(message)