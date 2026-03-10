package com.thehiveproject.event.api.controller

import com.thehiveproject.event.api.dto.*
import com.thehiveproject.event.api.mapper.toDTO
import com.thehiveproject.event.domain.booking.BookingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/bookings")
@Tag(
    name = "Bookings",
    description = "APIs for creating, viewing, and managing event bookings"
)
class BookingController(
    private val bookingService: BookingService
) {

    @Operation(
        summary = "Create a booking",
        description = "Creates a booking for the authenticated user for a specific event."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Booking created successfully",
                content = [Content(schema = Schema(implementation = BookingDTO::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid booking request"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "404", description = "User or Event not found"),
            ApiResponse(responseCode = "409", description = "Insufficient seats or event state conflict")
        ]
    )
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun createBooking(
        @Valid
        @RequestBody
        @Parameter(description = "Booking creation request", required = true)
        request: CreateBookingRequest,
    ): ResponseEntity<BookingDTO> {
        val userId = com.thehiveproject.event.infrastructure.security.SecurityUtils.getCurrentUserId()
        val response = bookingService.createBooking(request, userId)
        val responseDTO = response.toDTO()
        return ResponseEntity(responseDTO, HttpStatus.CREATED)
    }

    @Operation(
        summary = "Get my bookings",
        description = "Returns a paginated list of bookings for the authenticated user."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Bookings retrieved successfully",
                content = [Content(schema = Schema(implementation = PaginatedResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getMyBookings(
        @Parameter(description = "Pagination information")
        @PageableDefault(size = 10, sort = ["createdAt"])
        pageable: Pageable
    ): ResponseEntity<PaginatedResponse<BookingDTO>> {
        val userId = com.thehiveproject.event.infrastructure.security.SecurityUtils.getCurrentUserId()
        val bookings = bookingService.getMyBookings(userId, pageable)
        return ResponseEntity.ok(bookings.toPaginatedResponse())
    }

    @Operation(
        summary = "Update booking status",
        description = """
            Updates the status of a booking.
            - Users can only CANCEL their own bookings.
            - Admins can update any booking status.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Booking status updated successfully",
                content = [Content(schema = Schema(implementation = BookingDTO::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Access denied"),
            ApiResponse(responseCode = "404", description = "Booking not found"),
            ApiResponse(responseCode = "409", description = "Seat availability conflict")
        ]
    )
    @PatchMapping("/status/{id}")
    @PreAuthorize("isAuthenticated()")
    fun updateBookingStatus(
        @Parameter(description = "Booking ID", required = true)
        @PathVariable id: Long,
        @Valid
        @RequestBody
        @Parameter(description = "Booking status update request", required = true)
        request: UpdateBookingStatusRequest
    ): ResponseEntity<BookingDTO> {
        val userId = com.thehiveproject.event.infrastructure.security.SecurityUtils.getCurrentUserId()


        val response = bookingService.updateBookingStatus(
            id,
            request.status,
            userId,
        )

        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Payment webhook",
        description = """
            Handles payment provider webhooks (Stripe/PayPal).
            This endpoint is NOT authenticated and should be secured via signature verification.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            ApiResponse(responseCode = "400", description = "Invalid webhook payload"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @PostMapping("/webhook/payment")
    fun handlePaymentWebhook(
        @Valid
        @RequestBody
        @Parameter(description = "Payment webhook payload", required = true)
        payload: PaymentWebhookPayload
    ): ResponseEntity<String> {
        bookingService.processPaymentWebhook(payload)
        return ResponseEntity.ok("payment status processed successfully")
    }


    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('events:ROLE_ORGANIZER')")
    @Operation(
        summary = "Check in an attendee",
        description = "Validates a booking reference and marks the ticket as checked in. Only event organizers can perform this action.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Check-in successful",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CheckInResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid or already-used ticket",
                content = [Content(schema = Schema())]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Unauthorized – organizer does not own the event",
                content = [Content(schema = Schema())]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Booking not found",
                content = [Content(schema = Schema())]
            )
        ]
    )
    fun checkInAttendee(
        @RequestBody request: CheckInRequest
    ): ResponseEntity<CheckInResponse> {
        val userId = com.thehiveproject.event.infrastructure.security.SecurityUtils.getCurrentUserId()
        val res = bookingService.checkInAttendee(request, userId)
        return ResponseEntity.ok(
            res.toSanitized()
        )
    }
}
