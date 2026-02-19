package com.thehiveproject.event.domain.event.error

class EventNotFoundException(
 message: String = "Event not found"
): RuntimeException(message)