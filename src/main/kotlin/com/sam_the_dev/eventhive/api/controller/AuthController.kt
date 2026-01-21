package com.sam_the_dev.eventhive.api.controller

import com.sam_the_dev.eventhive.api.dto.AuthResponse
import com.sam_the_dev.eventhive.api.dto.LoginRequest
import com.sam_the_dev.eventhive.api.dto.RegisterUserDTO
import com.sam_the_dev.eventhive.api.dto.UserDTO
import com.sam_the_dev.eventhive.domain.auth.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody userDto: RegisterUserDTO): ResponseEntity<UserDTO> {
        val createdUser = authService.registerUser(userDto)
        return ResponseEntity(createdUser, HttpStatus.CREATED)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody loginDto: LoginRequest): ResponseEntity<AuthResponse> {
        val res = authService.login(loginDto)
        return ResponseEntity(res, HttpStatus.OK)
    }

}