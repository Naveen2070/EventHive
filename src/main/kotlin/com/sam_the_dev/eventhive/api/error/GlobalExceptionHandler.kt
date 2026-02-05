package com.sam_the_dev.eventhive.api.error

import com.sam_the_dev.eventhive.domain.auth.error.InvalidCredentialsException
import com.sam_the_dev.eventhive.domain.auth.error.TokenExpiredException
import com.sam_the_dev.eventhive.domain.auth.error.UnauthorizedUserException
import com.sam_the_dev.eventhive.domain.booking.error.InsufficientSeatsException
import com.sam_the_dev.eventhive.domain.booking.error.ResourceAccessDeniedException
import com.sam_the_dev.eventhive.domain.booking.error.TicketAlreadyUsedException
import com.sam_the_dev.eventhive.domain.booking.error.TicketInValidException
import com.sam_the_dev.eventhive.domain.event.error.*
import com.sam_the_dev.eventhive.domain.role.error.RoleNotFoundException
import com.sam_the_dev.eventhive.domain.user.error.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class GlobalExceptionHandler {

    // Initialize the logger
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // 1. Handle "Not Found" (404)
    @ExceptionHandler(
        UserNotFoundException::class,
        RoleNotFoundException::class,
        EventNotFoundException::class,
        TicketTierNotFoundException::class
    )
    fun handleNotFound(ex: RuntimeException, request: WebRequest): ResponseEntity<ApiErrorResponse> {
        val errorResponse = ApiErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message ?: "Resource not found",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    // 2. Handle Validation Errors or Bad Request (400)
    @ExceptionHandler(
        MethodArgumentNotValidException::class
    )
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {

        // Extract all error messages into a single list
        val errors = ex.bindingResult
            .allErrors
            .joinToString(", ")
            { error ->
                error.defaultMessage ?: "Invalid value"
            }

        val errorResponse = ApiErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = errors,
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    // 3. Handle ticket validation (400)
    @ExceptionHandler(
        TicketAlreadyUsedException::class,
        TicketInValidException::class,
        InvalidTicketTierException::class,
        InvalidResetTokenException::class,
        ExpiredResetTokenException::class,
        InvalidOldPasswordException::class,
    )
    fun handleInValidTicket(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {

        val errorResponse = ApiErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = ex.message ?: "Ticket Invalid",
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    // 4. Handle Authentication Errors (401)
    @ExceptionHandler(
        InvalidCredentialsException::class,
        TokenExpiredException::class,
        UnauthorizedUserException::class,
        UnauthorizedUserAccessException::class
    )
    fun handleInvalidCredentials(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {

        val errorResponse = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = HttpStatus.UNAUTHORIZED.reasonPhrase,
            message = ex.message ?: "Authentication failed",
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
    }

    // 5. Handle Forbidden Errors (403)
    @ExceptionHandler(
        UnauthorizedEventAccessException::class,
        ResourceAccessDeniedException::class,
        AccessDeniedException::class
    )
    fun handleUnauthorizedAccess(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errorResponse = ApiErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = HttpStatus.FORBIDDEN.reasonPhrase,
            message = ex.message ?: "Access denied",
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.FORBIDDEN)
    }

    // 6. Handle Business Rule Conflicts (409)
    @ExceptionHandler(
        UserAlreadyExistsException::class,
        EventNotPublishedException::class,
        EventAlreadyStartedException::class,
        InsufficientSeatsException::class,
        InsufficientSeatCapacityException::class,
        EventDateChangeNotAllowedException::class,
        EventModificationNotAllowedException::class
    )
    fun handleConflict(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {

        val errorResponse = ApiErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            error = HttpStatus.CONFLICT.reasonPhrase,
            message = ex.message ?: "Request conflicts with current resource state",
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
    }

    // 7. Handle Rate Limit (429)
    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimit(ex: RateLimitExceededException, request: WebRequest): ResponseEntity<ApiErrorResponse> {
        val errorResponse = ApiErrorResponse(
            status = HttpStatus.TOO_MANY_REQUESTS.value(),
            error = "Too Many Requests",
            message = ex.message ?: "Rate limit exceeded",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.TOO_MANY_REQUESTS)
    }

    // 8. Handle Everything Else (500)
    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<ApiErrorResponse> {
        // Log the real error internally so you can debug it later
        logger.error("Unexpected error", ex)

        val errorResponse = ApiErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = "An unexpected error occurred. Please try again later.",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}