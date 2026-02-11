package com.thehiveproject.event.application.auth

import com.thehiveproject.event.api.dto.AuthResponse
import com.thehiveproject.event.api.dto.LoginRequest
import com.thehiveproject.event.api.dto.RegisterUserDTO
import com.thehiveproject.event.domain.auth.AuthService
import com.thehiveproject.event.domain.auth.error.InvalidCredentialsException
import com.thehiveproject.event.domain.user.User
import com.thehiveproject.event.domain.user.UserService
import com.thehiveproject.event.infrastructure.security.JwtService
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthServiceImpl(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService,
    private val jwtService: JwtService
): AuthService {

    private val logger = LoggerFactory.getLogger(AuthServiceImpl::class.java)

    @Transactional
    override fun registerUser(user: RegisterUserDTO): User {
       return userService.registerUser(user)
    }

    override fun login(loginRequest: LoginRequest): AuthResponse {
        try {
            // 1. Authenticate user credentials
            val authenticationToken = UsernamePasswordAuthenticationToken(
                loginRequest.identifier,
                loginRequest.password
            )

            authenticationManager.authenticate(authenticationToken)

            // 2. Load user details
            val userDetails = userDetailsService.loadUserByUsername(loginRequest.identifier)
            val user = userService.getUserByEmailOrUsername(loginRequest.identifier)

            // 3. (Optional) Add custom JWT claims here
            val customClaims = mapOf<String, Any>(
                "id" to user.id!!,
                "email" to user.email,
                "username" to user.username
            )

            // 4. Generate JWT
            val token = jwtService.generateToken(customClaims, userDetails)

            // 5. Return response
            return AuthResponse(
                token = token,
                identifier = loginRequest.identifier
            )
        } catch (ex: AuthenticationException) {
            logger.error("AuthenticationException: {}", ex.message, ex)
            throw InvalidCredentialsException("Invalid email or password")
        }
    }
}