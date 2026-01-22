package com.sam_the_dev.eventhive.domain.booking.event

import com.sam_the_dev.eventhive.api.dto.BookingDTO

data class BookingSuccessEvent(
    val booking: BookingDTO,
    val userEmail: String
)