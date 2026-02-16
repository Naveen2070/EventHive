package com.thehiveproject.event.domain.booking

import com.thehiveproject.event.api.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BookingService {
    fun createBooking(request: CreateBookingRequest, token: String): Booking
    fun getMyBookings(token: String, pageable: Pageable): Page<BookingDTO>
    fun updateBookingStatus(bookingId: Long, newStatus: BookingStatus, token: String): BookingDTO
    fun processPaymentWebhook(payload: PaymentWebhookPayload)
    fun checkInAttendee(request: CheckInRequest, token: String): CheckInResponse
}