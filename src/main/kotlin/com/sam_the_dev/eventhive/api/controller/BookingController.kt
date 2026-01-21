package com.sam_the_dev.eventhive.api.controller

import com.sam_the_dev.eventhive.api.dto.*
import com.sam_the_dev.eventhive.domain.booking.BookingService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/bookings")
class BookingController(
    private val bookingService: BookingService
) {

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun createBooking(
        @Valid @RequestBody request: CreateBookingRequest,
        authentication: Authentication
    ): ResponseEntity<BookingDTO> {
        val userEmail = authentication.name

        val response = bookingService.createBooking(request, userEmail)

        return ResponseEntity(response, HttpStatus.CREATED)
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getMyBookings(
        authentication: Authentication,
        @PageableDefault(size = 10, sort = ["createdAt"]) pageable: Pageable
    ): ResponseEntity<PaginatedResponse<BookingDTO>> {
        val userEmail = authentication.name
        val bookings = bookingService.getMyBookings(userEmail, pageable)

        return ResponseEntity.ok(bookings.toPaginatedResponse())
    }

    @PatchMapping("/status/{id}")
    @PreAuthorize("isAuthenticated()")
    fun updateBookingStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateBookingStatusRequest,
        authentication: Authentication
    ): ResponseEntity<BookingDTO> {
        val isAdmin = authentication.authorities.any {
            it.authority == "ROLE_ADMIN" || it.authority == "ROLE_SUPER_ADMIN"
        }
        val response = bookingService.updateBookingStatus(id, request.status, authentication.name, isAdmin)
        return ResponseEntity.ok(response)
    }

    // ⚡ Webhook Endpoint
    // In a real scenario, this shouldn't use "PreAuthorize" because Stripe/PayPal calls it, not a logged-in user.
    // Instead, you secure this by validating a "Signature Header" secret.
    // For now, we will mark it permitAll() in SecurityConfig or leave it open for testing.
    @PostMapping("/webhook/payment")
    fun handlePaymentWebhook(@Valid @RequestBody payload: PaymentWebhookPayload): ResponseEntity<String> {
        bookingService.processPaymentWebhook(payload)
        return ResponseEntity.ok("payment status process successfully")
    }
}