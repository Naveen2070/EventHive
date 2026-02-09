package com.sam_the_dev.eventhive.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.sam_the_dev.eventhive.TestcontainersConfiguration
import com.sam_the_dev.eventhive.api.dto.CreateBookingRequest
import com.sam_the_dev.eventhive.api.dto.LoginRequest
import com.sam_the_dev.eventhive.api.dto.PaymentWebhookPayload
import com.sam_the_dev.eventhive.api.dto.UpdateBookingStatusRequest
import com.sam_the_dev.eventhive.domain.booking.BookingStatus
import com.sam_the_dev.eventhive.domain.event.EventStatus
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.BookingRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.event.TicketTierEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.TicketTierRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.role.RoleEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.role.RoleRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.role.UserRoleEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDateTime

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

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var roleRepository: RoleRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    // Test Data
    private lateinit var userToken: String
    private lateinit var adminToken: String
    private var eventId: Long = 0
    private var ticketTierId: Long = 0

    @BeforeEach
    fun setup() {
        // 1. Clean DB
        bookingRepository.deleteAll()
        ticketTierRepository.deleteAll()
        eventRepository.deleteAll()
        userRepository.deleteAll()

        // 2. Roles
        val roleAdmin = roleRepository.findByName("ADMIN")
            ?: roleRepository.save(RoleEntity(name = "ADMIN", createdBy = 0, updatedBy = 0))
        val roleUser = roleRepository.findByName("USER")
            ?: roleRepository.save(RoleEntity(name = "USER", createdBy = 0, updatedBy = 0))
        val roleOrg = roleRepository.findByName("ORGANIZER")
            ?: roleRepository.save(RoleEntity(name = "ORGANIZER", createdBy = 0, updatedBy = 0))

        // 3. Create Organizer & Event
        val organizer = userRepository.save(
            UserEntity(
                username = "org",
                email = "org@test.com",
                password = passwordEncoder.encode("x"),
                createdBy = 0L,
                updatedBy = 0L
            )
        )
        // Add role to organizer
        userRepository.save(organizer.apply {
            this.userRoles.add(UserRoleEntity(user = this, role = roleOrg, createdBy = 0, updatedBy = 0))
        })

        // Create Event
        var event = EventEntity(
            title = "Rock Concert",
            description = "Live",
            startDate = LocalDateTime.now().plusDays(5),
            endDate = LocalDateTime.now().plusDays(6),
            location = "Stadium",
            organizer = organizer,
            createdBy = organizer.id!!,
            updatedBy = organizer.id!!,
            status = EventStatus.PUBLISHED // Must be PUBLISHED to book
        )
        event = eventRepository.save(event)
        eventId = event.id!!

        // Create Ticket Tier
        var ticketTier = TicketTierEntity(
            name = "General",
            price = BigDecimal(100),
            totalAllocation = 10,
            availableAllocation = 10,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(10),
            event = event,
            createdBy = organizer.id!!,
            updatedBy = organizer.id!!
        )
        ticketTier = ticketTierRepository.save(ticketTier)
        ticketTierId = ticketTier.id!!

        // 4. Create Users & Tokens
        userToken = registerAndLogin("fan", "fan@test.com", "pass@123", roleUser)
        adminToken = registerAndLogin("admin", "admin@test.com", "pass@1234", roleAdmin)
    }

    private fun registerAndLogin(username: String, email: String, pass: String, role: RoleEntity): String {
        var user = UserEntity(
            username = username,
            email = email,
            password = passwordEncoder.encode(pass),
            createdBy = 0L,
            updatedBy = 0L
        )
        user = userRepository.save(user)
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
    fun `should create a booking successfully and decrease seats`() {
        // Need to provide ticketTierId now
        val request = CreateBookingRequest(eventId = eventId, ticketTierId = ticketTierId, ticketsCount = 2)

        mockMvc.post("/api/bookings") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("PENDING_PAYMENT") }
            jsonPath("$.ticketsCount") { value(2) }
        }.andReturn()

        // Verify Inventory in DB
        // NOTE: We need to check the TICKET TIER inventory, NOT the Event inventory directly if your logic is tier-based.
        // Assuming your updated logic (from ServiceImpl) decreases ticketTier.availableAllocation
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
        // 1. Create Booking (Seats = 8)
        val createReq = CreateBookingRequest(eventId = eventId, ticketTierId = ticketTierId, ticketsCount = 2)
        val createRes = mockMvc.post("/api/bookings") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createReq)
        }.andReturn()

        val bookingId = objectMapper.readTree(createRes.response.contentAsString).get("bookingId").asLong()

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

        // 3. Verify Seats Restored (Seats = 10)
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
        val bookingId = objectMapper.readTree(createRes.response.contentAsString).get("bookingId").asLong()

        // 2. User tries to confirm their own booking (Illegal!)
        val updateRequest = UpdateBookingStatusRequest(status = BookingStatus.CONFIRMED)

        mockMvc.patch("/api/bookings/status/$bookingId") {
            header("Authorization", "Bearer $userToken") // User Token
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isForbidden() } // Your Service throws ResourceAccessDeniedException which maps to 403 Forbidden
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
        val bookingId = objectMapper.readTree(createRes.response.contentAsString).get("bookingId").asLong()

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

        val ref = objectMapper.readTree(createRes.response.contentAsString).get("bookingReference").asText()
        val id = objectMapper.readTree(createRes.response.contentAsString).get("bookingId").asLong()

        // 2. Simulate Stripe Webhook
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
        // Need to fetch fresh from DB
        val booking = bookingRepository.findById(id).get()
        assertEquals(BookingStatus.CONFIRMED, booking.status)
    }
}