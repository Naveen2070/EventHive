package com.thehiveproject.event.domain.booking.event

import com.thehiveproject.event.api.dto.BookingDTO

data class BookingSuccessEvent(
    val booking: BookingDTO,
    val userEmail: String
)