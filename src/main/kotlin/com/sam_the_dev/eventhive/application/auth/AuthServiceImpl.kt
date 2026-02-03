package com.sam_the_dev.eventhive.application.auth

import com.sam_the_dev.eventhive.api.dto.AuthResponse
import com.sam_the_dev.eventhive.api.dto.LoginRequest
import com.sam_the_dev.eventhive.api.dto.RegisterUserDTO
import com.sam_the_dev.eventhive.api.dto.UserDTO
import com.sam_the_dev.eventhive.domain.auth.AuthService
import com.sam_the_dev.eventhive.domain.auth.error.InvalidCredentialsException
import com.sam_the_dev.eventhive.domain.user.UserService
import com.sam_the_dev.eventhive.infrastructure.security.JwtService
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
    override fun registerUser(user: RegisterUserDTO): UserDTO {
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