package com.sam_the_dev.eventhive.application.role

import com.sam_the_dev.eventhive.domain.role.error.RoleNotFoundException
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
import org.mockito.kotlin.*
import java.util.*

@ExtendWith(MockitoExtension::class)
class RoleServiceImplUnitTest {

    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var roleRepository: RoleRepository

    @InjectMocks
    lateinit var roleService: RoleServiceImpl

    // --- Helpers to create Real Entities (Better than mocking data classes) ---

    private fun createRole(name: String) = RoleEntity(
        id = 10L, name = name,
        createdBy = 0L,
        updatedBy = 0L
    )

    private fun createUser(id: Long = 1L): UserEntity {
        return UserEntity(
            id = id,
            username = "sam",
            email = "sam@test.com",
            password = "hash",
            createdBy = 0L,
            updatedBy = 0L
        )
    }

    // --- Tests for assignRoleToUser ---

    @Test
    fun `assignRoleToUser should add role and save user`() {
        // 1. Setup
        val user = createUser()
        val role = createRole("ROLE_ORGANIZER")

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(roleRepository.findByName("ROLE_ORGANIZER")).thenReturn(role)

        // Mock the save to return the modified user
        whenever(userRepository.save(any<UserEntity>())).thenAnswer { it.arguments[0] }

        // 2. Execute
        val result = roleService.assignRoleToUser(1L, "ROLE_ORGANIZER", 99L)

        // 3. Assert
        // Verify the entity passed to save() has the new role
        verify(userRepository).save(argThat { u ->
            u.userRoles.any { it.role.name == "ROLE_ORGANIZER" }
        })
        assertNotNull(result)
    }

    @Test
    fun `assignRoleToUser should do nothing if user already has role`() {
        // 1. Setup User WITH the role already
        val user = createUser()
        val role = createRole("ROLE_ORGANIZER")
        val existingMapping = UserRoleEntity(
            user = user, role = role,
            createdBy = 0L,
            updatedBy = 0L
        )
        user.userRoles.add(existingMapping)

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(roleRepository.findByName("ROLE_ORGANIZER")).thenReturn(role)

        // 2. Execute
        roleService.assignRoleToUser(1L, "ROLE_ORGANIZER", 99L)

        // 3. Assert
        // Should NOT save because nothing changed
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `assignRoleToUser should throw UserNotFoundException`() {
        whenever(userRepository.findById(any())).thenReturn(Optional.empty())

        assertThrows(UserNotFoundException::class.java) {
            roleService.assignRoleToUser(1L, "ROLE_ADMIN", 99L)
        }
    }

    @Test
    fun `assignRoleToUser should throw RoleNotFoundException`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
        whenever(roleRepository.findByName("fake_role")).thenReturn(null)

        assertThrows(RoleNotFoundException::class.java) {
            roleService.assignRoleToUser(1L, "fake_role", 99L)
        }
    }

    // --- Tests for removeRoleFromUser ---

    @Test
    fun `removeRoleFromUser should soft delete the role mapping`() {
        // 1. Setup User WITH Role
        val user = createUser()
        val role = createRole("ROLE_ADMIN")
        val mapping = UserRoleEntity(
            user = user, role = role,
            createdBy = 0L,
            updatedBy = 99L
        )
        user.userRoles.add(mapping)

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        // Important: Mock save to return the same object passed to it
        whenever(userRepository.save(any<UserEntity>())).thenAnswer { it.arguments[0] }

        // 2. Execute
        roleService.removeRoleFromUser(1L, "ROLE_ADMIN", 99L)

        // 3. Assert using 'check' for detailed errors
        verify(userRepository).save(check { savedUser ->
            // Find the specific role mapping
            val roleMap = savedUser.userRoles.find { it.role.name == "ROLE_ADMIN" }

            assertNotNull(roleMap, "Role mapping was lost during save!")
            assertTrue(roleMap!!.isDeleted, "isDeleted should be true")
            assertEquals(99L, roleMap.updatedBy, "updatedBy should match the admin ID")
        })
    }

    @Test
    fun `removeRoleFromUser should throw exception if role not active or deleted`() {
        // 1. Setup User WITH Role
        val user = createUser()
        val role = createRole("ROLE_ADMIN")
        val mapping = UserRoleEntity(
            user = user, role = role,
            createdBy = 0L,
            updatedBy = 99L
        )
        mapping.markDeleted(99L)
        user.userRoles.add(mapping)

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

        // 2. Execute & Assert
        val ex = assertThrows(RoleNotFoundException::class.java) {
            roleService.removeRoleFromUser(1L, "ROLE_ADMIN", 99L)
        }

        // Should contain the specific error message
        assertTrue(ex.message!!.contains("Role ROLE_ADMIN not found or already deleted"))
    }


    @Test
    fun `removeRoleFromUser should throw exception if role not found on user`() {
        // 1. Setup User WITHOUT roles
        val user = createUser() // empty roles list

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

        // 2. Execute & Assert
        val ex = assertThrows(RoleNotFoundException::class.java) {
            roleService.removeRoleFromUser(1L, "ROLE_ADMIN", 99L)
        }

        // Should contain the specific error message
        assertTrue(ex.message!!.contains("Role ROLE_ADMIN not found or already deleted"))

        verify(userRepository, never()).save(any())
    }

    @Test
    fun `removeRoleFromUser should throw exception if DB save fails`() {
        // 1. Setup
        val user = createUser()
        val role = createRole("ROLE_X")
        user.userRoles.add(UserRoleEntity(
            user = user, role = role,
            createdBy = 0L,
            updatedBy = 0L
        ))

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        // Simulate DB Crash
        whenever(userRepository.save(any<UserEntity>())).thenThrow(RuntimeException("DB connection failed"))

        // 2. Execute & Assert
        val ex = assertThrows(RuntimeException::class.java) {
            roleService.removeRoleFromUser(1L, "ROLE_X", 99L)
        }

        assertTrue(ex.message!!.contains("unable to soft delete role"))
    }
}