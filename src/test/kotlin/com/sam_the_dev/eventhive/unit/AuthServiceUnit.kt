package com.sam_the_dev.eventhive.unit

import com.sam_the_dev.eventhive.api.dto.LoginRequest
import com.sam_the_dev.eventhive.api.dto.RegisterUserDTO
import com.sam_the_dev.eventhive.api.dto.UserDTO
import com.sam_the_dev.eventhive.application.auth.AuthServiceImpl
import com.sam_the_dev.eventhive.domain.auth.error.InvalidCredentialsException
import com.sam_the_dev.eventhive.domain.user.UserService
import com.sam_the_dev.eventhive.infrastructure.security.JwtService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import java.time.Instant

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

        val expectedUser = UserDTO(
            id = 1L,
            username = "sam",
            email = "sam@test.com",
            roles = setOf("USER"),
            createdBy = 0L,
            updatedBy = 0L,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            deletedAt = null,
            isActive = true,
            isDeleted = false,
        )

        `when`(userService.registerUser(registerRequest)).thenReturn(expectedUser)

        val result = authService.registerUser(registerRequest)

        assertEquals(expectedUser, result)
        verify(userService).registerUser(registerRequest)
    }

    @Test
    fun `login should return token on successful authentication`() {
        val loginRequest = LoginRequest("sam@test.com", "password")

        val userDetails: UserDetails = User.builder()
            .username("sam@test.com")
            .password("encoded_password")
            .roles("USER")
            .build()

        val generatedToken = "jwt_token_123"

        `when`(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenReturn(null) // just indicate success

        `when`(userDetailsService.loadUserByUsername("sam@test.com")).thenReturn(userDetails)
        `when`(jwtService.generateToken(any(), eq(userDetails))).thenReturn(generatedToken)

        val response = authService.login(loginRequest)

        assertEquals(generatedToken, response.token)
        assertEquals("sam@test.com", response.identifier)

        verify(authenticationManager).authenticate(any<UsernamePasswordAuthenticationToken>())
        verify(userDetailsService).loadUserByUsername("sam@test.com")
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
    }
}
