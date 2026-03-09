package com.thehiveproject.event.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.thehiveproject.event.TestcontainersConfiguration
import com.thehiveproject.event.api.dto.CreateTicketTierRequest
import com.thehiveproject.event.api.dto.UpdateTicketTierRequest
import com.thehiveproject.event.domain.event.EventStatus
import com.thehiveproject.event.infrastructure.persistence.event.EventEntity
import com.thehiveproject.event.infrastructure.persistence.event.EventRepository
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierEntity
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
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
class TicketTierControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var ticketTierRepository: TicketTierRepository

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    private lateinit var organizerToken: String
    private lateinit var adminToken: String
    private lateinit var userToken: String
    private lateinit var otherOrganizerToken: String
    private lateinit var movieOrganizerToken: String

    private val organizerId: Long = 100
    private val adminId: Long = 999
    private val userId: Long = 50
    private val otherOrganizerId: Long = 101
    private val movieOrganizerId: Long = 102

    private var eventId: Long = 0

    @BeforeEach
    fun setup() {
        ticketTierRepository.deleteAll()
        eventRepository.deleteAll()

        organizerToken = generateTestToken(organizerId, "org@test.com", mapOf("events" to listOf("ORGANIZER")))
        adminToken = generateTestToken(adminId, "admin@test.com", mapOf("events" to listOf("ADMIN")))
        userToken = generateTestToken(userId, "user@test.com", mapOf("events" to listOf("USER")))
        otherOrganizerToken =
            generateTestToken(otherOrganizerId, "other@test.com", mapOf("events" to listOf("ORGANIZER")))
        movieOrganizerToken =
            generateTestToken(movieOrganizerId, "movie@test.com", mapOf("movies" to listOf("ORGANIZER")))

        // 11 params for EventEntity
        val event = EventEntity(
            null, // id
            "Test Event", // title
            "Desc", // description
            LocalDateTime.now().plusDays(10), // startDate
            LocalDateTime.now().plusDays(11), // endDate
            "Location", // location
            mutableListOf(), // ticketTiers
            EventStatus.DRAFT, // status
            organizerId, // organizerId
            organizerId, // createdBy
            organizerId // updatedBy
        )
        eventId = eventRepository.save(event).id!!
    }

    private fun generateTestToken(id: Long, email: String, permissions: Map<String, List<String>>): String {
        val keyBytes = Decoders.BASE64.decode(jwtSecret)
        val key: SecretKey = Keys.hmacShaKeyFor(keyBytes)

        // Extract flattened roles for legacy 'roles' claim
        val roles = permissions.values.flatten().map { if (it.startsWith("ROLE_")) it else "ROLE_$it" }

        return Jwts.builder()
            .subject(email)
            .claim("id", id)
            .claim("roles", roles)
            .claim("permissions", permissions)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 1000 * 60 * 60))
            .signWith(key)
            .compact()
    }

    @Test
    fun `should allow ORGANIZER with events domain permission to add tier`() {

        val event = eventRepository.findById(eventId).get()

        val request = CreateTicketTierRequest(
            name = "VIP",
            price = BigDecimal("100.00"),
            totalAllocation = 10,
            validFrom = event.startDate.plusHours(1),
            validUntil = event.startDate.plusHours(5),
            createdBy = organizerId
        )

        mockMvc.post("/api/tiers/events/$eventId") {
            header("Authorization", "Bearer $organizerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun `should forbid ORGANIZER from different domain (movies) from adding tier to events`() {
        val request = CreateTicketTierRequest(
            name = "VIP",
            price = BigDecimal("100.00"),
            totalAllocation = 10,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(5),
            createdBy = movieOrganizerId
        )

        mockMvc.post("/api/tiers/events/$eventId") {
            header("Authorization", "Bearer $movieOrganizerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isForbidden() }
        }
    }


    @Test
    fun `should allow ADMIN to add tier even if they don't own the event`() {

        val event = eventRepository.findById(eventId).get()

        val request = CreateTicketTierRequest(
            name = "Admin Tier",
            price = BigDecimal("0.00"),
            totalAllocation = 100,
            validFrom = event.startDate.plusHours(1),
            validUntil = event.startDate.plusHours(5),
            createdBy = adminId
        )

        mockMvc.post("/api/tiers/events/$eventId") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun `should forbid one organizer from updating another organizer's tier`() {
        val eventObj = eventRepository.findById(eventId).get()
        // 10 params for TicketTierEntity
        val tier = TicketTierEntity(
            null, // id
            "Early Bird", // name
            BigDecimal("50.00"), // price
            100, // totalAllocation
            100, // availableAllocation
            LocalDateTime.now(), // validFrom
            LocalDateTime.now().plusDays(5), // validUntil
            eventObj, // event
            organizerId, // createdBy
            organizerId // updatedBy
        )

        val tierId = ticketTierRepository.save(tier).id!!

        val updateRequest = UpdateTicketTierRequest(
            name = "Hacked Tier",
            price = BigDecimal("10.00"),
            totalAllocation = 50,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(5),
            updatedBy = otherOrganizerId
        )

        mockMvc.put("/api/tiers/$tierId") {
            header("Authorization", "Bearer $otherOrganizerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should return 404 when adding tier to non-existent event`() {
        val request = CreateTicketTierRequest(
            name = "Ghost Tier",
            price = BigDecimal("10.00"),
            totalAllocation = 10,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(5),
            createdBy = organizerId
        )

        mockMvc.post("/api/tiers/events/999999") {
            header("Authorization", "Bearer $organizerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should return 400 for invalid input (negative price)`() {
        val request = CreateTicketTierRequest(
            name = "Free?",
            price = BigDecimal("-1.00"), // Invalid
            totalAllocation = 10,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(5),
            createdBy = organizerId
        )

        mockMvc.post("/api/tiers/events/$eventId") {
            header("Authorization", "Bearer $organizerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
