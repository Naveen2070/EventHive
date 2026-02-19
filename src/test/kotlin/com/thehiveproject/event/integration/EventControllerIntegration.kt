package com.thehiveproject.event.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.thehiveproject.event.TestcontainersConfiguration
import com.thehiveproject.event.api.dto.CreateEventRequest
import com.thehiveproject.event.api.dto.CreateTicketTierRequest
import com.thehiveproject.event.api.dto.UpdateEventRequest
import com.thehiveproject.event.api.dto.UserSummaryDTO
import com.thehiveproject.event.domain.event.EventStatus
import com.thehiveproject.event.infrastructure.persistence.client.IdentityClient
import com.thehiveproject.event.infrastructure.persistence.event.EventEntity
import com.thehiveproject.event.infrastructure.persistence.event.EventRepository
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierEntity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.crypto.SecretKey

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var eventRepository: EventRepository

    // Inject a MockBean to bypass real Feign HTTP calls during integration tests
    @MockitoBean
    lateinit var identityClient: IdentityClient

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    // Tokens
    private lateinit var organizerToken: String
    private lateinit var adminToken: String
    private lateinit var userToken: String
    private lateinit var otherOrganizerToken: String

    // Test Data - Arbitrary IDs since we don't have a User DB
    private val organizerId: Long = 100
    private val adminId: Long = 999
    private val userId: Long = 50
    private val otherOrganizerId: Long = 101

    @BeforeEach
    fun setup() {
        // 1. Clean DB
        eventRepository.deleteAll()

        // 2. Stub the Feign client responses so all integration test flows work
        val mockUser = UserSummaryDTO(organizerId, "Test Organizer", "org@test.com")
        `when`(identityClient.getUsersById(any())).thenReturn(mockUser)
        `when`(identityClient.getUsersByIds(any())).thenReturn(listOf(mockUser))

        // 3. Generate Stateless Tokens (Simulating Identity Service)
        organizerToken = generateTestToken(organizerId, "org@test.com", listOf("ROLE_ORGANIZER"))
        adminToken = generateTestToken(adminId, "admin@test.com", listOf("ROLE_ADMIN"))
        userToken = generateTestToken(userId, "user@test.com", listOf("ROLE_USER"))
        otherOrganizerToken = generateTestToken(otherOrganizerId, "other@test.com", listOf("ROLE_ORGANIZER"))
    }

    private fun generateTestToken(id: Long, email: String, roles: List<String>): String {
        val keyBytes = Decoders.BASE64.decode(jwtSecret)
        val key: SecretKey = Keys.hmacShaKeyFor(keyBytes)

        return Jwts.builder()
            .subject(email)
            .claim("id", id)
            .claim("roles", roles)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 1000 * 60 * 60))
            .signWith(key)
            .compact()
    }

    @Test
    fun `should allow ORGANIZER to create an event with tiers`() {
        val tierReq = CreateTicketTierRequest(
            name = "VIP",
            price = BigDecimal("299.99"),
            totalAllocation = 100,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(10),
            createdBy = organizerId
        )

        val request = CreateEventRequest(
            title = "Tech Summit 2024",
            description = "Big Tech Event",
            startDate = LocalDateTime.now().plusDays(10),
            endDate = LocalDateTime.now().plusDays(11),
            location = "San Francisco",
            ticketTiers = listOf(tierReq),
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
            jsonPath("$.ticketTiers[0].name") { value("VIP") }
            jsonPath("$.ticketTiers[0].availableAllocation") { value(100) }
        }
    }

    @Test
    fun `should forbid normal USER from creating event`() {
        val tierReq = CreateTicketTierRequest(
            name = "General",
            price = BigDecimal("10.00"),
            totalAllocation = 10,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(2),
            createdBy = userId
        )

        val request = CreateEventRequest(
            title = "Illegal Event",
            description = "Desc",
            startDate = LocalDateTime.now().plusDays(1),
            endDate = LocalDateTime.now().plusDays(2),
            location = "Nowhere",
            ticketTiers = listOf(tierReq),
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
    fun `should filter events by location`() {
        // Setup: Create 2 events manually in DB using IDs
        val event1 = EventEntity(
            title = "NY Concert", description = "Loud",
            startDate = LocalDateTime.now().plusDays(5), endDate = LocalDateTime.now().plusDays(6),
            location = "New York", status = EventStatus.PUBLISHED,
            organizerId = organizerId, createdBy = organizerId, updatedBy = organizerId
        )
        event1.ticketTiers.add(
            TicketTierEntity(
                name = "VIP", price = BigDecimal("100.00"), totalAllocation = 50, availableAllocation = 50,
                validFrom = LocalDateTime.now(), validUntil = LocalDateTime.now().plusDays(10),
                event = event1, createdBy = organizerId, updatedBy = organizerId
            )
        )
        eventRepository.save(event1)

        val event2 = EventEntity(
            title = "London Theatre", description = "Quiet",
            startDate = LocalDateTime.now().plusDays(5), endDate = LocalDateTime.now().plusDays(6),
            location = "London", status = EventStatus.PUBLISHED,
            organizerId = organizerId, createdBy = organizerId, updatedBy = organizerId
        )
        event2.ticketTiers.add(
            TicketTierEntity(
                name = "General", price = BigDecimal("50.00"), totalAllocation = 50, availableAllocation = 50,
                validFrom = LocalDateTime.now(), validUntil = LocalDateTime.now().plusDays(10),
                event = event2, createdBy = organizerId, updatedBy = organizerId
            )
        )
        eventRepository.save(event2)

        // 1. Filter by Location: "New York"
        mockMvc.get("/api/events?location=New York") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content[0].title") { value("NY Concert") }
        }
    }

    @Test
    fun `organizer should update their own event successfully`() {
        // 1. Create Event
        var event = EventEntity(
            title = "Old Title", description = "Desc",
            startDate = LocalDateTime.now().plusDays(5), endDate = LocalDateTime.now().plusDays(6),
            location = "Home", status = EventStatus.PUBLISHED,
            organizerId = organizerId, createdBy = organizerId, updatedBy = organizerId
        )
        event = eventRepository.save(event)

        // 2. Update Request
        val updateRequest = UpdateEventRequest(
            title = "New Title",
            description = null, location = null, startDate = null, endDate = null
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
        var event = EventEntity(
            title = "Org1 Event", description = "Desc",
            startDate = LocalDateTime.now().plusDays(5), endDate = LocalDateTime.now().plusDays(6),
            location = "Home", status = EventStatus.PUBLISHED,
            organizerId = organizerId, createdBy = organizerId, updatedBy = organizerId
        )
        event = eventRepository.save(event)

        // 2. Org2 (different ID) tries to update it
        val updateRequest = UpdateEventRequest(
            title = "Hacked Title",
            description = null, location = null, startDate = null, endDate = null
        )

        mockMvc.put("/api/events/${event.id}") {
            header("Authorization", "Bearer $otherOrganizerToken") // Org 2 Token
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            // Your controller throws UnauthorizedEventAccessException -> 403 Forbidden
            status { isForbidden() }
        }
    }

    @Test
    fun `admin should be able to soft delete any event`() {
        // 1. Org creates event
        var event = EventEntity(
            title = "To Be Deleted", description = "Desc",
            startDate = LocalDateTime.now().plusDays(5),
            endDate = LocalDateTime.now().plusDays(6),
            location = "Home",
            status = EventStatus.PUBLISHED,
            organizerId = organizerId,
            createdBy = organizerId,
            updatedBy = organizerId
        )
        event = eventRepository.save(event)

        // 2. Admin deletes it
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