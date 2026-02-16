package com.thehiveproject.event.unit

import com.thehiveproject.event.api.dto.ChangePasswordRequest
import com.thehiveproject.event.api.dto.UpdateUserRequest
import com.thehiveproject.event.application.user.UserServiceImpl
import com.thehiveproject.event.domain.user.error.*
import com.thehiveproject.event.domain.user.event.PasswordChangedEvent
import com.thehiveproject.event.domain.user.event.PasswordResetInitiatedEvent
import com.thehiveproject.event.infrastructure.persistence.role.RoleEntity
import com.thehiveproject.event.infrastructure.persistence.role.RoleRepository
import com.thehiveproject.event.infrastructure.persistence.role.UserRoleEntity
import com.thehiveproject.event.infrastructure.persistence.user.PasswordResetTokenEntity
import com.thehiveproject.event.infrastructure.persistence.user.PasswordResetTokenRepository
import com.thehiveproject.event.infrastructure.persistence.user.UserEntity
import com.thehiveproject.event.infrastructure.persistence.user.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserServiceUnitTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    @Mock
    lateinit var roleRepository: RoleRepository

    @Mock
    lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Mock
    lateinit var eventPublisher: ApplicationEventPublisher

    @InjectMocks
    lateinit var userService: UserServiceImpl

    // --- Helper Data ---
    private val registerDTO = RegisterUserDTO("sam", "sam@test.com", "pass123")
    private val roleUser = RoleEntity(
        id = 1L, name = "USER", createdBy = 0L, updatedBy = 0L
    )

    private fun createMockUserEntity(
        id: Long = 100L,
        username: String = "sam",
        email: String = "sam@test.com",
        password: String = "encoded_pass123"
    ): UserEntity {
        val entity = UserEntity(
            id = id,
            username = username,
            email = email,
            password = password,
            createdBy = 0L,
            updatedBy = 0L
        )
        entity.userRoles.add(
            UserRoleEntity(
                user = entity,
                role = roleUser,
                id = 1L, createdBy = 0L, updatedBy = 0L
            )
        )
        return entity
    }

    // --- Register Tests ---

    @Test
    fun `registerUser should save user successfully`() {
        whenever(userRepository.findByUsernameOrEmail(any(), any())).thenReturn(null)
        whenever(roleRepository.findByName("USER")).thenReturn(roleUser)
        whenever(passwordEncoder.encode("pass123")).thenReturn("encoded_pass123")
        whenever(userRepository.save(any<UserEntity>())).thenAnswer {
            (it.arguments[0] as UserEntity).apply { id = 1L }
        }

        val result = userService.registerUser(registerDTO)

        assertNotNull(result)
        assertEquals("sam", result.username)
        assertEquals("sam@test.com", result.email)
        verify(userRepository).save(any())
    }

    @Test
    fun `registerUser should throw exception if user already exists`() {
        whenever(userRepository.findByUsernameOrEmail("sam", "sam@test.com"))
            .thenReturn(createMockUserEntity())

        assertThrows(UserAlreadyExistsException::class.java) {
            userService.registerUser(registerDTO)
        }
        verify(userRepository, never()).save(any())
    }

    // --- Get User Tests ---

    @Test
    fun `getUserById should return user if found`() {
        val entity = createMockUserEntity()
        whenever(userRepository.findById(100L)).thenReturn(Optional.of(entity))

        val result = userService.getUserById(100L)

        assertEquals(100L, result.id)
        assertEquals("sam", result.username)
    }

    @Test
    fun `getUserById should throw UserNotFoundException if empty`() {
        whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

        val ex = assertThrows(UserNotFoundException::class.java) {
            userService.getUserById(999L)
        }
        assertTrue(ex.message!!.contains("User not found"))
    }

    // --- Update User Tests ---

    @Test
    fun `updateUser should update username successfully`() {
        val entity = createMockUserEntity()
        val request = UpdateUserRequest(username = "new_sam")

        whenever(userRepository.findById(100L)).thenReturn(Optional.of(entity))
        whenever(userRepository.existsByUsername("new_sam")).thenReturn(false)
        whenever(userRepository.save(any<UserEntity>())).thenAnswer { it.arguments[0] }

        val result = userService.updateUser(100L, request, "sam@test.com")

        assertEquals("new_sam", result.username)
        verify(userRepository).save(entity)
    }

    @Test
    fun `updateUser should throw UnauthorizedUserAccessException if updating another user`() {
        val entity = createMockUserEntity(email = "victim@test.com", username = "victim")
        val request = UpdateUserRequest(username = "hacker")

        whenever(userRepository.findById(100L)).thenReturn(Optional.of(entity))

        assertThrows(UnauthorizedUserAccessException::class.java) {
            // Logged in as "attacker@test.com", trying to update "victim@test.com"
            userService.updateUser(100L, request, "attacker@test.com")
        }
    }

    @Test
    fun `updateUser should throw UserAlreadyExistsException if new username taken`() {
        val entity = createMockUserEntity()
        val request = UpdateUserRequest(username = "taken_name")

        whenever(userRepository.findById(100L)).thenReturn(Optional.of(entity))
        whenever(userRepository.existsByUsername("taken_name")).thenReturn(true)

        assertThrows(UserAlreadyExistsException::class.java) {
            userService.updateUser(100L, request, "sam@test.com")
        }
    }

    // --- Change Password Tests ---

    @Test
    fun `changePassword should succeed with correct old password`() {
        val entity = createMockUserEntity(password = "encoded_old")
        val request = ChangePasswordRequest("old_pass", "new_pass")

        whenever(userRepository.findById(100L)).thenReturn(Optional.of(entity))
        whenever(passwordEncoder.matches("old_pass", "encoded_old")).thenReturn(true)
        whenever(passwordEncoder.encode("new_pass")).thenReturn("encoded_new")

        userService.changePassword(100L, request, "sam@test.com")

        verify(userRepository).save(check {
            assertEquals("encoded_new", it.password)
        })
        verify(eventPublisher).publishEvent(any<PasswordChangedEvent>())
    }

    @Test
    fun `changePassword should fail with incorrect old password`() {
        val entity = createMockUserEntity(password = "encoded_old")
        val request = ChangePasswordRequest("wrong_pass", "new_pass")

        whenever(userRepository.findById(100L)).thenReturn(Optional.of(entity))
        whenever(passwordEncoder.matches("wrong_pass", "encoded_old")).thenReturn(false)

        assertThrows(InvalidOldPasswordException::class.java) {
            userService.changePassword(100L, request, "sam@test.com")
        }
        verify(userRepository, never()).save(any())
    }

    // --- Password Reset Tests ---

    @Test
    fun `initiatePasswordReset should create token and publish event`() {
        val entity = createMockUserEntity()
        whenever(userRepository.findByEmail("sam@test.com")).thenReturn(entity)
        whenever(passwordResetTokenRepository.save(any<PasswordResetTokenEntity>())).thenAnswer { it.arguments[0] }

        userService.initiatePasswordReset("sam@test.com")

        verify(passwordResetTokenRepository).deleteByUser_Id(100L)
        verify(passwordResetTokenRepository).save(any())
        verify(eventPublisher).publishEvent(any<PasswordResetInitiatedEvent>())
    }

    @Test
    fun `completePasswordReset should update password given valid token`() {
        val user = createMockUserEntity()
        val tokenEntity = PasswordResetTokenEntity(
            token = "valid_token",
            user = user,
            expiryDate = LocalDateTime.now().plusHours(1)
        )

        whenever(passwordResetTokenRepository.findByToken("valid_token")).thenReturn(Optional.of(tokenEntity))
        whenever(passwordEncoder.encode("new_pass")).thenReturn("encoded_new")

        userService.completePasswordReset("valid_token", "new_pass")

        verify(userRepository).save(check {
            assertEquals("encoded_new", it.password)
        })
        verify(passwordResetTokenRepository).delete(tokenEntity)
        verify(eventPublisher).publishEvent(any<PasswordChangedEvent>())
    }

    @Test
    fun `completePasswordReset should fail if token expired`() {
        val user = createMockUserEntity()
        val expiredToken = PasswordResetTokenEntity(
            token = "expired_token",
            user = user,
            expiryDate = LocalDateTime.now().minusMinutes(1)
        )

        whenever(passwordResetTokenRepository.findByToken("expired_token")).thenReturn(Optional.of(expiredToken))

        assertThrows(ExpiredResetTokenException::class.java) {
            userService.completePasswordReset("expired_token", "new_pass")
        }
        verify(passwordResetTokenRepository).delete(expiredToken)
        verify(userRepository, never()).save(any())
    }
}