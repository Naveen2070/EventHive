package com.sam_the_dev.eventhive.domain.event.error

class EventNotFoundException(
 message: String = "Event not found"
): RuntimeException(message)