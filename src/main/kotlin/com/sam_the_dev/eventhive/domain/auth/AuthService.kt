package com.sam_the_dev.eventhive.domain.auth

import com.sam_the_dev.eventhive.api.dto.AuthResponse
import com.sam_the_dev.eventhive.api.dto.LoginRequest
import com.sam_the_dev.eventhive.api.dto.RegisterUserDTO
import com.sam_the_dev.eventhive.domain.user.User

interface AuthService {
    fun registerUser(user: RegisterUserDTO): User
    fun login(loginRequest: LoginRequest): AuthResponse
}