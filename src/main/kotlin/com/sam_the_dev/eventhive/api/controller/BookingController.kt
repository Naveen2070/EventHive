package com.sam_the_dev.eventhive.api.controller

import com.sam_the_dev.eventhive.api.dto.BookingDTO
import com.sam_the_dev.eventhive.api.dto.CreateBookingRequest
import com.sam_the_dev.eventhive.api.dto.PaginatedResponse
import com.sam_the_dev.eventhive.api.dto.toPaginatedResponse
import com.sam_the_dev.eventhive.domain.booking.BookingService
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
        @RequestBody request: CreateBookingRequest,
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
}