package com.thehiveproject.event.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.thehiveproject.event.TestcontainersConfiguration
import com.thehiveproject.event.api.dto.LoginRequest
import com.thehiveproject.event.api.dto.RoleAssignmentRequest
import com.thehiveproject.event.infrastructure.persistence.role.RoleRepository
import com.thehiveproject.event.infrastructure.persistence.role.UserRoleEntity
import com.thehiveproject.event.infrastructure.persistence.user.UserEntity
import com.thehiveproject.event.infrastructure.persistence.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import kotlin.test.assertTrue

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var roleRepository: RoleRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    private lateinit var adminToken: String
    private var targetUserId: Long = 0
    private var adminUserId: Long = 0

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()

        // 1. Get Roles
        val roleAdmin = roleRepository.findByName("ADMIN")!!
        val roleUser = roleRepository.findByName("USER")!!


        // 2. Create an Admin User (To perform the actions)
        val adminUser = UserEntity(
                username = "admin",
                email = "admin@test.com",
                password = passwordEncoder.encode("admin123"),
                createdBy = 0L,
                updatedBy = 0L
        )
        val savedAdmin = userRepository.save(adminUser)
        val adminRole = UserRoleEntity(role = roleAdmin, user = savedAdmin, createdBy = 0L, updatedBy = 0L)
        savedAdmin.userRoles.add(adminRole)
        userRepository.save(savedAdmin)
        adminUserId = savedAdmin.id!!

        // 3. Create a Target User (To receive the roles)
        val targetUser = UserEntity(
                username = "target",
                email = "target@test.com",
                password = passwordEncoder.encode("user123"),
                createdBy = 0L,
                updatedBy = 0L
        )
        targetUserId = userRepository.save(targetUser).id!!
        val targetRole = UserRoleEntity(role = roleUser, user = targetUser, createdBy = 0L, updatedBy = 0L)
        targetUser.userRoles.add(targetRole)
        userRepository.save(targetUser)

        // 4. Log in as Admin to get Token
        val loginResult = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("admin@test.com", "admin123"))
        }.andReturn()

        adminToken = objectMapper.readTree(loginResult.response.contentAsString).get("token").asText()
    }

    @Test
    fun `should assign ORGANIZER to a user successfully`() {
        val request = RoleAssignmentRequest(
                roleName = "ORGANIZER",
                updateBy = adminUserId
        )

        mockMvc.put("/api/admin/roles/assign/$targetUserId") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.roles") {
                isArray()
                value(org.hamcrest.Matchers.hasItem("ORGANIZER"))
            }

        }

        // Verify in DB
        val updatedUser = userRepository.findById(targetUserId).get()
        assertTrue { updatedUser.userRoles.map { roleEntity -> roleEntity.role.name }.contains("ORGANIZER") }
    }

    @Test
    fun `should remove role (downgrade to USER) successfully`() {
        // 1. First, make the target user an ORGANIZER manually
        val organizerRole = roleRepository.findByName("ORGANIZER")!!
                val user = userRepository.findById(targetUserId).get()
        val organizerRoleEntity = UserRoleEntity(role = organizerRole, user = user, createdBy = 0L, updatedBy = 0L)
        user.userRoles.add(organizerRoleEntity)
        userRepository.save(user)

        // 2. Request to remove ORGANIZER (Logic depends on your service: usually resets to USER or null)
        val request = RoleAssignmentRequest(
                roleName = "ORGANIZER",
                updateBy = adminUserId
        )

        mockMvc.delete("/api/admin/roles/remove/$targetUserId") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            // Assuming your service logic defaults back to USER upon removal
            jsonPath("$.roles") {
                isArray()
                value(org.hamcrest.Matchers.hasItem("USER"))
            }
        }
    }

    @Test
    fun `should fail with 403 Forbidden if non-admin tries to assign role`() {
        // 1. Create and Login as a standard user
        val userRegister = RegisterUserDTO("hacker", "hacker@test.com", "pass123")
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(userRegister)
        }

        val loginResult = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("hacker@test.com", "pass123"))
        }.andReturn()

        val userToken = objectMapper.readTree(loginResult.response.contentAsString).get("token").asText()

        // 2. Try to access Admin API
        val request = RoleAssignmentRequest(roleName = "ORGANIZER", updateBy = 0L)

        mockMvc.put("/api/admin/roles/assign/$targetUserId") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should return 404 if user not found`() {
        val request = RoleAssignmentRequest(roleName = "ORGANIZER", updateBy = adminUserId)

        mockMvc.put("/api/admin/roles/assign/999999") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isNotFound() }
        }
    }
}