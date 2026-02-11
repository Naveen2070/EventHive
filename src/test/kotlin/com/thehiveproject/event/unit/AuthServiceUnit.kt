package com.thehiveproject.event.unit

import com.thehiveproject.event.api.dto.LoginRequest
import com.thehiveproject.event.api.dto.RegisterUserDTO
import com.thehiveproject.event.application.auth.AuthServiceImpl
import com.thehiveproject.event.domain.auth.error.InvalidCredentialsException
import com.thehiveproject.event.domain.user.User
import com.thehiveproject.event.domain.user.UserService
import com.thehiveproject.event.infrastructure.security.JwtService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.User as SecurityUser

@ExtendWith(MockitoExtension::class)
class AuthServiceUnitTest {

    @Mock
    lateinit var userService: UserService

    @Mock
    lateinit var authenticationManager: AuthenticationManager

    @Mock
    lateinit var userDetailsService: UserDetailsService

    @Mock
    lateinit var jwtService: JwtService

    @InjectMocks
    lateinit var authService: AuthServiceImpl

    @Test
    fun `registerUser should delegate to userService`() {
        val registerRequest = RegisterUserDTO(
            username = "sam",
            email = "sam@test.com",
            password = "password",
        )

        val expectedUser = User(
            id = 1L,
            username = "sam",
            email = "sam@test.com",
            password = "encoded_password",
            createdBy = 0L,
            updatedBy = 0L
        )

        `when`(userService.registerUser(registerRequest)).thenReturn(expectedUser)

        val result = authService.registerUser(registerRequest)

        assertEquals(expectedUser, result)
        verify(userService).registerUser(registerRequest)
    }

    @Test
    fun `login should return token on successful authentication`() {
        val loginRequest = LoginRequest("sam@test.com", "password")

        // 1. Setup Spring Security UserDetails (for UserDetailsService)
        val userDetails: UserDetails = SecurityUser.builder()
            .username("sam@test.com")
            .password("encoded_password")
            .roles("USER")
            .build()

        // 2. Setup Domain User (for UserService - REQUIRED for custom claims)
        val domainUser = User(
            id = 100L,
            username = "sam",
            email = "sam@test.com",
            password = "encoded_password",
            createdBy = 0L,
            updatedBy = 0L
        )

        val generatedToken = "jwt_token_123"

        // Mock 1: Authentication Manager
        `when`(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenReturn(mock(Authentication::class.java))

        // Mock 2: UserDetailsService
        `when`(userDetailsService.loadUserByUsername("sam@test.com")).thenReturn(userDetails)

        // Mock 3: UserService
        `when`(userService.getUserByEmailOrUsername("sam@test.com")).thenReturn(domainUser)

        // Mock 4: JwtService
        `when`(jwtService.generateToken(any(), eq(userDetails))).thenReturn(generatedToken)

        // Act
        val response = authService.login(loginRequest)

        // Assert
        assertEquals(generatedToken, response.token)
        assertEquals("sam@test.com", response.identifier)

        verify(authenticationManager).authenticate(any<UsernamePasswordAuthenticationToken>())
        verify(userDetailsService).loadUserByUsername("sam@test.com")
        verify(userService).getUserByEmailOrUsername("sam@test.com")
        verify(jwtService).generateToken(any(), eq(userDetails))
    }

    @Test
    fun `login should throw InvalidCredentialsException when authentication fails`() {
        val loginRequest = LoginRequest("wrong@test.com", "wrong_pass")

        `when`(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenThrow(BadCredentialsException("Bad credentials"))

        val exception = assertThrows(InvalidCredentialsException::class.java) {
            authService.login(loginRequest)
        }

        assertEquals("Invalid email or password", exception.message)

        verifyNoInteractions(jwtService)
        verifyNoInteractions(userService)
    }
}