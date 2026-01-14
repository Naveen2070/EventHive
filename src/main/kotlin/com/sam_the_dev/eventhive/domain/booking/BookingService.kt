package com.sam_the_dev.eventhive.domain.booking

import com.sam_the_dev.eventhive.api.dto.BookingDTO
import com.sam_the_dev.eventhive.api.dto.CreateBookingRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BookingService {
    fun createBooking(request: CreateBookingRequest, userEmail: String): BookingDTO
    fun getMyBookings(userEmail: String, pageable: Pageable): Page<BookingDTO>
}