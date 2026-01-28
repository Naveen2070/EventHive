package com.sam_the_dev.eventhive.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.sam_the_dev.eventhive.TestcontainersConfiguration
import com.sam_the_dev.eventhive.api.dto.CreateEventRequest
import com.sam_the_dev.eventhive.api.dto.LoginRequest
import com.sam_the_dev.eventhive.api.dto.UpdateEventRequest
import com.sam_the_dev.eventhive.domain.event.EventStatus
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.role.RoleEntity
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
import org.springframework.test.web.servlet.*
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertTrue

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var roleRepository: RoleRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    // Tokens
    private lateinit var organizerToken: String
    private lateinit var adminToken: String
    private lateinit var userToken: String

    // Test Data
    private var organizerId: Long = 0
    private var userId: Long = 0

    @BeforeEach
    fun setup() {
        // 1. Clean DB
        eventRepository.deleteAll()
        userRepository.deleteAll()

        // 2. Get Roles
        val roleOrg = roleRepository.findByName("ORGANIZER")!!
        val roleAdmin = roleRepository.findByName("ADMIN")!!
        val roleUser = roleRepository.findByName("USER")!!

        // 3. Create Users & Get Tokens
        organizerToken = registerAndLogin("org", "org@test.com", "pass@123", roleOrg)
        organizerId = userRepository.findByUsernameOrEmail("org@test.com", "org@test.com")!!.id!!

        adminToken = registerAndLogin("admin", "admin@test.com", "pass@1234", roleAdmin)
        userToken = registerAndLogin("user", "user@test.com", "pass@12345", roleUser)
        userId = userRepository.findByUsernameOrEmail("user@test.com","user@test.com")!!.id!!
    }

    // Helper to register, login, and return JWT
    private fun registerAndLogin(username: String, email: String, pass: String, role: RoleEntity): String {
        val user = UserEntity(
            username = username,
            email = email,
            password = passwordEncoder.encode(pass),
            createdBy = 0L,
            updatedBy = 0L
        )
        userRepository.save(user)
        user.userRoles.add(
            UserRoleEntity(
                user = user,
                role = role,
                createdBy = 0L,
                updatedBy = 0L
            )
        )
        userRepository.save(user)

        val loginResult = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest(email, pass))
        }.andReturn()

        return objectMapper.readTree(loginResult.response.contentAsString).get("token").asText()
    }

    @Test
    fun `should allow ORGANIZER to create an event`() {
        val request = CreateEventRequest(
            title = "Tech Summit 2024",
            description = "Big Tech Event",
            startDate = LocalDateTime.now().plusDays(10),
            endDate = LocalDateTime.now().plusDays(11),
            location = "San Francisco",
            price = BigDecimal("299.99"),
            totalSeats = 100,
            organizerEmail = "org@test.com",
            createdBy = organizerId
        )

        mockMvc.post("/api/events") {
            header("Authorization", "Bearer $organizerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.title") { value("Tech Summit 2024") }
            jsonPath("$.availableSeats") { value(100) }
        }
    }

    @Test
    fun `should forbid normal USER from creating event`() {
        val request = CreateEventRequest(
            title = "Illegal Event",
            description = "Desc",
            startDate = LocalDateTime.now().plusDays(1),
            endDate = LocalDateTime.now().plusDays(2),
            location = "Nowhere",
            price = BigDecimal("10.00"),
            totalSeats = 10,
            organizerEmail = "user@test.com",
            createdBy = userId
        )

        mockMvc.post("/api/events") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should filter events by location and price`() {
        // Setup: Create 2 events manually in DB
        val orgUser = userRepository.findById(organizerId).get()

        eventRepository.save(EventEntity(
            title = "NY Concert", description = "Loud",
            startDate = LocalDateTime.now().plusDays(5), endDate = LocalDateTime.now().plusDays(6),
            location = "New York", price = BigDecimal("100.00"),
            totalSeats = 50, availableSeats = 50, status = EventStatus.PUBLISHED,
            organizer = orgUser,
            createdBy = 0L,
            updatedBy = 0L
        ))

        eventRepository.save(EventEntity(
            title = "London Theatre", description = "Quiet",
            startDate = LocalDateTime.now().plusDays(5), endDate = LocalDateTime.now().plusDays(6),
            location = "London", price = BigDecimal("50.00"),
            totalSeats = 50, availableSeats = 50, status = EventStatus.PUBLISHED,
            organizer = orgUser,
            createdBy = 0L,
            updatedBy = 0L
        ))

        // 1. Filter by Location: "New York"
        mockMvc.get("/api/events?location=New York") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content[0].title") { value("NY Concert") }
        }

        // 2. Filter by Price < 80
        mockMvc.get("/api/events?maxPrice=80") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content[0].title") { value("London Theatre") }
        }
    }

    @Test
    fun `organizer should update their own event successfully`() {
        // 1. Create Event
        val orgUser = userRepository.findById(organizerId).get()
        val event = eventRepository.save(EventEntity(
            title = "Old Title", description = "Desc",
            startDate = LocalDateTime.now().plusDays(5), endDate = LocalDateTime.now().plusDays(6),
            location = "Home", price = BigDecimal("10.00"),
            totalSeats = 10, availableSeats = 10, status = EventStatus.PUBLISHED,
            organizer = orgUser,
            createdBy = 0L,
            updatedBy = 0L
        ))

        // 2. Update Request
        val updateRequest = UpdateEventRequest(
            title = "New Title",
            description = null, location = null, price = null, totalSeats = null, startDate = null, endDate = null
        )

        mockMvc.put("/api/events/${event.id}") {
            header("Authorization", "Bearer $organizerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("New Title") }
        }
    }

    @Test
    fun `should prevent one organizer from updating another organizer's event`() {
        // 1. Org1 creates event
        val orgUser = userRepository.findById(organizerId).get()
        val event = eventRepository.save(EventEntity(
            title = "Org1 Event", description = "Desc",
            startDate = LocalDateTime.now().plusDays(5), endDate = LocalDateTime.now().plusDays(6),
            location = "Home", price = BigDecimal("10.00"),
            totalSeats = 10, availableSeats = 10, status = EventStatus.PUBLISHED,
            organizer = orgUser,
            createdBy = 0L,
            updatedBy = 0L
        ))

        // 2. Org2 tries to update it
        // We create a second organizer on the fly
        val roleOrg = roleRepository.findByName("ORGANIZER")!!
        val org2Token = registerAndLogin("org2", "org2@test.com", "pass@123", roleOrg)

        val updateRequest = UpdateEventRequest(title = "Hacked Title", description = null, location = null, price = null, totalSeats = null, startDate = null, endDate = null)

        mockMvc.put("/api/events/${event.id}") {
            header("Authorization", "Bearer $org2Token") // Different Token
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            // Depending on implementation, this is either 403 Forbidden or 404 Not Found (if using ownership filter)
            // Or a custom ResourceAccessDeniedException
            status { isForbidden() }
        }
    }

    @Test
    fun `admin should be able to soft delete any event`() {
        // 1. Org creates event
        val orgUser = userRepository.findById(organizerId).get()
        val event = eventRepository.save(EventEntity(
            title = "To Be Deleted", description = "Desc",
            startDate = LocalDateTime.now().plusDays(5),
            endDate = LocalDateTime.now().plusDays(6),
            location = "Home",
            price = BigDecimal("10.00"),
            totalSeats = 10,
            availableSeats = 10,
            status = EventStatus.PUBLISHED,
            organizer = orgUser,
            createdBy = 0L,
            updatedBy = 0L
        ))

        // 2. Admin deletes it (soft delete)
        mockMvc.delete("/api/events/${event.id}") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isNoContent() }
        }

        // 3. Verify soft delete
        val deletedEvent = eventRepository.findById(event.id!!).get()
        assertTrue(deletedEvent.isDeleted, "Event should be marked as deleted")
    }
}