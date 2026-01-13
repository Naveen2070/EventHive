package com.sam_the_dev.eventhive.domain.event.error

class InsufficientSeatCapacityException (
    message: String = "Cannot reduce total seats below the number of already sold tickets."
) : RuntimeException(message)