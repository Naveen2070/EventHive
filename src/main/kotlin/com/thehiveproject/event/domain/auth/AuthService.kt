package com.thehiveproject.event.domain.auth

import com.thehiveproject.event.api.dto.AuthResponse
import com.thehiveproject.event.api.dto.LoginRequest
import com.thehiveproject.event.api.dto.RegisterUserDTO
import com.thehiveproject.event.domain.user.User

interface AuthService {
    fun registerUser(user: RegisterUserDTO): User
    fun login(loginRequest: LoginRequest): AuthResponse
}