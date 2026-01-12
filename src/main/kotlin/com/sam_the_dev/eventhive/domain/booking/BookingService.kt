package com.sam_the_dev.eventhive.domain.booking

import com.sam_the_dev.eventhive.api.dto.BookingDTO
import com.sam_the_dev.eventhive.api.dto.CreateBookingRequest

interface BookingService {
    fun createBooking(request: CreateBookingRequest, userEmail: String): BookingDTO
}