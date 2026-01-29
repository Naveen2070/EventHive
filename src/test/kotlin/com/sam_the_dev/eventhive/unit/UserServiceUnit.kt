package com.sam_the_dev.eventhive.unit

import com.sam_the_dev.eventhive.api.dto.RegisterUserDTO
import com.sam_the_dev.eventhive.application.user.UserServiceImpl
import com.sam_the_dev.eventhive.domain.user.error.UserAlreadyExistsException
import com.sam_the_dev.eventhive.domain.user.error.UserNotFoundException
import com.sam_the_dev.eventhive.infrastructure.persistence.role.RoleEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.role.RoleRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.role.UserRoleEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserServiceUnitTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    @Mock
    lateinit var roleRepository: RoleRepository

    @InjectMocks
    lateinit var userService: UserServiceImpl

    // --- Helper Data ---
    private val registerDTO = RegisterUserDTO("sam", "sam@test.com", "pass123")
    private val roleUser = RoleEntity(
        id = 1L, name = "USER",
        createdBy = 0L,
        updatedBy = 0L
    )

    // A dummy entity to represent what's in the DB
    private fun createMockUserEntity(): UserEntity {
        val entity = UserEntity(
            id = 100L,
            username = "sam",
            email = "sam@test.com",
            password = "encoded_pass123",
            createdBy = 0L,
            updatedBy = 0L
        )
        // Add roles if your Entity logic requires it for toDomain() to work
         entity.userRoles.add(UserRoleEntity(
             user = entity,
             role = roleUser,
             id = 1L,
             createdBy = 0L,
             updatedBy = 0L,
         ))
        return entity
    }

    // --- Test Cases ---

    @Test
    fun `registerUser should save user successfully`() {
        // 1. Mock: User does not exist
        whenever(userRepository.findByUsernameOrEmail(any(), any())).thenReturn(null)

        // 2. Mock: Role exists
        whenever(roleRepository.findByName("USER")).thenReturn(roleUser)

        // 3. Mock: Password Encoding
        whenever(passwordEncoder.encode("pass123")).thenReturn("encoded_pass123")

        // 4. Mock: Saving returns the entity with an ID
        whenever(userRepository.save(any<UserEntity>())).thenAnswer { invocation ->
            val entity = invocation.arguments[0] as UserEntity
            entity
        }

        // Execute
        val result = userService.registerUser(registerDTO)

        // Assert
        assertNotNull(result)
        assertEquals("sam", result.username)
        assertEquals("sam@test.com", result.email)

        // Verify Interactions
        verify(passwordEncoder).encode("pass123")
        verify(userRepository).save(any())
    }

    @Test
    fun `registerUser should throw exception if user already exists`() {
        // Mock: User DOES exist
        whenever(userRepository.findByUsernameOrEmail("sam", "sam@test.com"))
            .thenReturn(createMockUserEntity())

        // Execute & Assert
        assertThrows(UserAlreadyExistsException::class.java) {
            userService.registerUser(registerDTO)
        }

        // Verify we never tried to save
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `registerUser should throw exception if default ROLE not found`() {
        // Mock: User doesn't exist
        whenever(userRepository.findByUsernameOrEmail(any(), any())).thenReturn(null)

        // Mock: Role is missing (Critical config error)
        whenever(roleRepository.findByName("USER")).thenReturn(null)

        // Execute & Assert
        val ex = assertThrows(RuntimeException::class.java) {
            userService.registerUser(registerDTO)
        }
        assertEquals("Default Role 'USER' not found in database", ex.message)
    }

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
        // Assuming your exception message format
        assertTrue(ex.message!!.contains("User not found with id=999"))
    }

    @Test
    fun `getUserByEmailOrUsername should return Domain object if found`() {
        val entity = createMockUserEntity()
        whenever(userRepository.findByUsernameOrEmail("sam", "sam")).thenReturn(entity)

        val result = userService.getUserByEmailOrUsername("sam")

        // Note: This returns a Domain User object, not DTO
        assertEquals("sam", result.username)
        assertEquals("encoded_pass123", result.password)
    }

    @Test
    fun `getUserByEmailOrUsername should throw exception if not found`() {
        whenever(userRepository.findByUsernameOrEmail("unknown", "unknown")).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            userService.getUserByEmailOrUsername("unknown")
        }
    }
}