package com.sam_the_dev.eventhive.domain.booking

import com.sam_the_dev.eventhive.api.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BookingService {
    fun createBooking(request: CreateBookingRequest, userEmail: String): Booking
    fun getMyBookings(userEmail: String, pageable: Pageable): Page<BookingDTO>
    fun updateBookingStatus(bookingId: Long, newStatus: BookingStatus, userEmail: String, isAdmin: Boolean): BookingDTO
    fun processPaymentWebhook(payload: PaymentWebhookPayload)
    fun checkInAttendee(request: CheckInRequest, userEmail: String): CheckInResponse
}