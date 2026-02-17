package com.thehiveproject.event.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.thehiveproject.event.TestcontainersConfiguration
import com.thehiveproject.event.api.dto.BookingDTO
import com.thehiveproject.event.api.dto.CreateBookingRequest
import com.thehiveproject.event.api.dto.PaymentWebhookPayload
import com.thehiveproject.event.api.dto.UpdateBookingStatusRequest
import com.thehiveproject.event.domain.booking.BookingStatus
import com.thehiveproject.event.domain.event.EventStatus
import com.thehiveproject.event.infrastructure.persistence.booking.BookingRepository
import com.thehiveproject.event.infrastructure.persistence.event.EventEntity
import com.thehiveproject.event.infrastructure.persistence.event.EventRepository
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierEntity
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Date
import javax.crypto.SecretKey

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BookingControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var ticketTierRepository: TicketTierRepository

    // Read the secret from test properties to sign tokens
    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    // Test Data
    private lateinit var userToken: String
    private lateinit var adminToken: String
    private var eventId: Long = 0
    private var ticketTierId: Long = 0
    private val organizerId: Long = 2222
    private val userId: Long = 101
    private val adminId: Long = 999

    @BeforeEach
    fun setup() {
        // 1. Clean DB
        bookingRepository.deleteAll()
        ticketTierRepository.deleteAll()
        eventRepository.deleteAll()

        // 2. Create Event
        var event = EventEntity(
            title = "Rock Concert",
            description = "Live",
            startDate = LocalDateTime.now().plusDays(5),
            endDate = LocalDateTime.now().plusDays(6),
            location = "Stadium",
            organizerId = organizerId,
            status = EventStatus.PUBLISHED,
            createdBy = organizerId,
            updatedBy = organizerId
        )
        event = eventRepository.save(event)
        eventId = event.id!!

        // 3. Create Ticket Tier
        var ticketTier = TicketTierEntity(
            name = "General",
            price = BigDecimal(100),
            totalAllocation = 10,
            availableAllocation = 10,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(10),
            event = event,
            createdBy = organizerId,
            updatedBy = organizerId
        )
        ticketTier = ticketTierRepository.save(ticketTier)
        ticketTierId = ticketTier.id!!

        // 4. Generate Tokens (Stateless - No DB insert needed!)
        userToken = generateTestToken(userId, "fan@test.com", listOf("ROLE_USER"))
        adminToken = generateTestToken(adminId, "admin@test.com", listOf("ROLE_ADMIN"))
    }

    // Helper to generate a valid JWT for testing
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
    fun `should create a booking successfully and decrease seats`() {
        val request = CreateBookingRequest(eventId = eventId, ticketTierId = ticketTierId, ticketsCount = 2)

        mockMvc.post("/api/bookings") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("PENDING_PAYMENT") }
            jsonPath("$.ticketsCount") { value(2) }
        }

        // Verify Inventory in DB
        val updatedTier = ticketTierRepository.findById(ticketTierId).get()
        assertEquals(8, updatedTier.availableAllocation, "Should have 8 seats left (10 - 2)")
    }

    @Test
    fun `should list my bookings`() {
        // 1. Create a booking first
        val request = CreateBookingRequest(eventId = eventId, ticketTierId = ticketTierId, ticketsCount = 1)
        mockMvc.post("/api/bookings") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect { status { isCreated() } }

        // 2. Fetch list
        mockMvc.get("/api/bookings") {
            header("Authorization", "Bearer $userToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content[0].eventTitle") { value("Rock Concert") }
        }
    }

    @Test
    fun `user should be able to CANCEL their own booking`() {
        // 1. Create Booking
        val createReq = CreateBookingRequest(eventId = eventId, ticketTierId = ticketTierId, ticketsCount = 2)
        val createRes = mockMvc.post("/api/bookings") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createReq)
        }.andReturn()

        val createResponse = objectMapper.readValue(
            createRes.response.contentAsString,
            BookingDTO::class.java
        )

        val bookingId = createResponse.bookingId

        // 2. Cancel Booking
        val updateRequest = UpdateBookingStatusRequest(status = BookingStatus.CANCELLED)

        mockMvc.patch("/api/bookings/status/$bookingId") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CANCELLED") }
        }

        // 3. Verify Seats Restored
        val tier = ticketTierRepository.findById(ticketTierId).get()
        assertEquals(10, tier.availableAllocation, "Seats should be restored after cancellation")
    }

    @Test
    fun `user cannot change booking to CONFIRMED (only Admin)`() {
        // 1. Create Booking
        val createRes = mockMvc.post("/api/bookings") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateBookingRequest(eventId, ticketTierId, 1))
        }.andReturn()

        val createResponse = objectMapper.readValue(
            createRes.response.contentAsString,
            BookingDTO::class.java
        )

        val bookingId = createResponse.bookingId


        // 2. User tries to confirm their own booking
        val updateRequest = UpdateBookingStatusRequest(status = BookingStatus.CONFIRMED)

        mockMvc.patch("/api/bookings/status/$bookingId") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isForbidden() } // 403 Forbidden
        }
    }

    @Test
    fun `admin should be able to change status to CONFIRMED`() {
        // 1. Create Booking
        val createRes = mockMvc.post("/api/bookings") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateBookingRequest(eventId, ticketTierId, 1))
        }.andReturn()

        val createResponse = objectMapper.readValue(
            createRes.response.contentAsString,
            BookingDTO::class.java
        )

        val bookingId = createResponse.bookingId


        // 2. Admin Confirms it
        val updateRequest = UpdateBookingStatusRequest(status = BookingStatus.CONFIRMED)

        mockMvc.patch("/api/bookings/status/$bookingId") {
            header("Authorization", "Bearer $adminToken") // Admin Token
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CONFIRMED") }
        }
    }

    @Test
    fun `webhook should process payment success`() {
        // 1. Create Booking (PENDING_PAYMENT)
        val createRes = mockMvc.post("/api/bookings") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateBookingRequest(eventId, ticketTierId, 1))
        }.andReturn()

        val createResponse = objectMapper.readValue(
            createRes.response.contentAsString,
            BookingDTO::class.java
        )

        val ref = createResponse.bookingReference
        val id = createResponse.bookingId

        // 2. Simulate Webhook
        val webhookPayload = PaymentWebhookPayload(
            bookingReference = ref,
            paymentId = "pay_123",
            status = "SUCCESS"
        )

        mockMvc.post("/api/bookings/webhook/payment") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(webhookPayload)
        }.andExpect {
            status { isOk() }
        }

        // 3. Verify Booking is now CONFIRMED
        val booking = bookingRepository.findById(id).get()
        assertEquals(BookingStatus.CONFIRMED, booking.status)
    }
}