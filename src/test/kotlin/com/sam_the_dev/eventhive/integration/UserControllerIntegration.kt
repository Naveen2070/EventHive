package com.sam_the_dev.eventhive.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.sam_the_dev.eventhive.TestcontainersConfiguration
import com.sam_the_dev.eventhive.api.dto.LoginRequest
import com.sam_the_dev.eventhive.infrastructure.persistence.role.RoleRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.role.UserRoleEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var roleRepository: RoleRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    private var testUserId: Long = 0
    private lateinit var authToken: String

    @BeforeEach
    fun setup() {
        // 1. Cleanup Database
        userRepository.deleteAll()

        // 2. Create Role
        val roleUser = roleRepository.findByName("USER")!!

        // 3. Create a Test User in DB
        val user = UserEntity(
            username = "john_doe",
            email = "john@example.com",
            password = passwordEncoder.encode("password123"),
            createdBy = 0L,
            updatedBy = 0L
        )
        val savedUser = userRepository.save(user)
        savedUser.userRoles.add(
            UserRoleEntity(
                 role = roleUser,
                user = savedUser,
                createdBy = 0L,
                updatedBy = 0L
            )
        )
        testUserId = savedUser.id!!

        // 4. Log in to get a valid JWT Token
        val loginResult = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("john@example.com", "password123"))
        }.andReturn()

        authToken = objectMapper.readTree(loginResult.response.contentAsString).get("token").asText()
    }

    @Test
    fun `should retrieve user details successfully when authenticated`() {
        // Note: URL matches your controller: /api/user + /users/{id}
        mockMvc.get("/api/user/users/$testUserId") {
            header("Authorization", "Bearer $authToken")
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.id") { value(testUserId) }
            jsonPath("$.username") { value("john_doe") }
            jsonPath("$.email") { value("john@example.com") }
            // Verify sensitive data like password is NOT returned (good practice)
            jsonPath("$.password") { doesNotExist() }
            jsonPath("$.passwordHash") { doesNotExist() }
        }
    }

    @Test
    fun `should return 404 Not Found for non-existent user ID`() {
        val nonExistentId = 999999L

        mockMvc.get("/api/user/users/$nonExistentId") {
            header("Authorization", "Bearer $authToken")
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
            // Optional: Check error message structure if your global exception handler returns one
            // jsonPath("$.error") { value("Not Found") }
        }
    }

    @Test
    fun `should return 401 Unauthorized when no token is provided`() {
        mockMvc.get("/api/user/users/$testUserId") {
            contentType = MediaType.APPLICATION_JSON
            // No Authorization header
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `should return 401 Unauthorized when token is invalid`() {
        mockMvc.get("/api/user/users/$testUserId") {
            header("Authorization", "Bearer invalid_junk_token")
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}