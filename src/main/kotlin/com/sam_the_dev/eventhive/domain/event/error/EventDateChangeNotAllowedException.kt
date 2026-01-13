package com.sam_the_dev.eventhive.domain.event.error

class EventDateChangeNotAllowedException(
    message: String = "Cannot change event dates because tickets have already been sold. Please cancel and create a new event."
) : RuntimeException(message)