package com.thehiveproject.event.domain.booking

import com.thehiveproject.event.api.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BookingService {
    fun createBooking(request: CreateBookingRequest, userId: Long): Booking
    fun getMyBookings(userId: Long, pageable: Pageable): Page<BookingDTO>
    fun updateBookingStatus(bookingId: Long, newStatus: BookingStatus, userId: Long): BookingDTO
    fun processPaymentWebhook(payload: PaymentWebhookPayload)
    fun checkInAttendee(request: CheckInRequest, userId: Long): CheckInResponse
}