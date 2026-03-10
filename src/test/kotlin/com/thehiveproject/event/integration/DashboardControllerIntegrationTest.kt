package com.thehiveproject.event.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.thehiveproject.event.TestcontainersConfiguration
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
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.*
import javax.crypto.SecretKey

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DashboardControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    private lateinit var organizerToken: String
    private lateinit var userToken: String
    private lateinit var movieOrganizerToken: String
    private lateinit var expiredToken: String

    private val organizerId: Long = 100
    private val userId: Long = 50
    private val movieOrganizerId: Long = 101

    @BeforeEach
    fun setup() {
        organizerToken = generateTestToken(organizerId, "org@test.com", mapOf("events" to listOf("ORGANIZER")))
        userToken = generateTestToken(userId, "user@test.com", mapOf("events" to listOf("USER")))
        movieOrganizerToken =
            generateTestToken(movieOrganizerId, "movie@test.com", mapOf("movies" to listOf("ORGANIZER")))

        // Generate an expired token
        val keyBytes = Decoders.BASE64.decode(jwtSecret)
        val key: SecretKey = Keys.hmacShaKeyFor(keyBytes)
        expiredToken = Jwts.builder()
            .subject("expired@test.com")
            .claim("id", 999L)
            .claim("permissions", mapOf("events" to listOf("ORGANIZER")))
            .issuedAt(Date(System.currentTimeMillis() - 10000))
            .expiration(Date(System.currentTimeMillis() - 5000))
            .signWith(key)
            .compact()
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
    fun `should allow ORGANIZER with events domain permission to access dashboard`() {
        mockMvc.get("/api/dashboard/stats") {
            header("Authorization", "Bearer $organizerToken")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `should forbid normal USER from accessing dashboard`() {
        mockMvc.get("/api/dashboard/stats") {
            header("Authorization", "Bearer $userToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should forbid ORGANIZER from different domain (movies) from accessing events dashboard`() {
        mockMvc.get("/api/dashboard/stats") {
            header("Authorization", "Bearer $movieOrganizerToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should return 401 when token is missing`() {
        mockMvc.get("/api/dashboard/stats")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `should return 401 when token is expired`() {
        mockMvc.get("/api/dashboard/stats") {
            header("Authorization", "Bearer $expiredToken")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `should return 401 when token is malformed`() {
        mockMvc.get("/api/dashboard/stats") {
            header("Authorization", "Bearer not.a.valid.token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
