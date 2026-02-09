package com.sam_the_dev.eventhive.api.controller

import com.sam_the_dev.eventhive.api.dto.*
import com.sam_the_dev.eventhive.api.mapper.toDTO
import com.sam_the_dev.eventhive.domain.auth.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(
    name = "Authentication",
    description = "APIs for authentication operations"
)
class AuthController(
    private val authService: AuthService
) {

    @Operation(
        summary = "Register a new user",
        description = "Registers a new user in the system and returns the created user's information."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "User registered successfully",
                content = [Content(schema = Schema(implementation = UserDTO::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid registration request"
            ),
            ApiResponse(
                responseCode = "409",
                description = "User already exists"
            )
        ]
    )
    @PostMapping("/register")
    fun register(
        @Valid
        @RequestBody
        @Parameter(description = "User registration payload", required = true)
        userDto: RegisterUserDTO
    ): ResponseEntity<UserDTO> {
        val createdUser = authService.registerUser(userDto)
        val userDTO = createdUser.toDTO()
        return ResponseEntity(userDTO, HttpStatus.CREATED)
    }

    @Operation(
        summary = "Login a user",
        description = "Authenticates a user with email/username and password, returning a JWT token upon success."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User authenticated successfully",
                content = [Content(schema = Schema(implementation = AuthResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid login request"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials"
            )
        ]
    )
    @PostMapping("/login")
    fun login(
        @Valid
        @RequestBody
        @Parameter(description = "Login request payload", required = true)
        loginDto: LoginRequest
    ): ResponseEntity<AuthResponse> {
        val res = authService.login(loginDto)
        val sanitizedRes = res.sanitizedForHtml()
        return ResponseEntity(sanitizedRes, HttpStatus.OK)
    }
}
