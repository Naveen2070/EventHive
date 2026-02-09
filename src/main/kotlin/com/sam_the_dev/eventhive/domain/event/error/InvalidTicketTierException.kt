package com.sam_the_dev.eventhive.domain.event.error

class InvalidTicketTierException (
    message: String = "Ticket Tier does not belong to this Event"
):RuntimeException(message)