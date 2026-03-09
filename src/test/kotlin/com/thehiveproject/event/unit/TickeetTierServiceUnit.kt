package com.thehiveproject.event.unit

import com.thehiveproject.event.api.dto.CreateTicketTierRequest
import com.thehiveproject.event.application.event.TicketTierServiceImpl
import com.thehiveproject.event.domain.event.EventStatus
import com.thehiveproject.event.domain.event.error.UnauthorizedEventAccessException
import com.thehiveproject.event.infrastructure.persistence.event.EventEntity
import com.thehiveproject.event.infrastructure.persistence.event.EventRepository
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierEntity
import com.thehiveproject.event.infrastructure.persistence.event.TicketTierRepository
import com.thehiveproject.event.infrastructure.security.JwtService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class TicketTierServiceUnitTest {

    @Mock
    lateinit var ticketTierRepository: TicketTierRepository

    @Mock
    lateinit var eventRepository: EventRepository

    @Mock
    lateinit var jwtService: JwtService

    @InjectMocks
    lateinit var ticketTierService: TicketTierServiceImpl

    // --- Helpers ---
    private val organizerId = 1L
    private val adminId = 999L

    @BeforeEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    private fun setupMockUser(userId: Long, roles: List<String> = emptyList()) {
        val authorities = roles.map { SimpleGrantedAuthority(it) }
        val auth = UsernamePasswordAuthenticationToken("user@test.com", userId, authorities)
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = auth
        SecurityContextHolder.setContext(context)
    }

    private fun createEvent(
        startDate: LocalDateTime = LocalDateTime.now(),
        endDate: LocalDateTime = LocalDateTime.now().plusDays(10)
    ) = EventEntity(
        id = 100L,
        title = "Test Event",
        startDate = startDate,
        endDate = endDate,
        location = "Venue",
        organizerId = organizerId,
        description = "Desc",
        createdBy = 1L,
        updatedBy = 1L,
        status = EventStatus.DRAFT
    )

    // --- Add Tier Tests ---

    @Test
    fun `addTierToEvent should succeed when valid`() {
        setupMockUser(organizerId, listOf("events:ROLE_ORGANIZER"))
        val event = createEvent()
        val request = CreateTicketTierRequest(
            name = "VIP",
            price = BigDecimal("100.00"),
            totalAllocation = 50,
            validFrom = event.startDate.plusDays(1),
            validUntil = event.endDate.minusDays(1),
            createdBy = 1L
        )

        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))
        whenever(ticketTierRepository.save(any<TicketTierEntity>())).thenAnswer {
            (it.arguments[0] as TicketTierEntity).apply { id = 55L }
        }

        val result = ticketTierService.addTierToEvent(100L, request, organizerId)

        assertNotNull(result)
        assertEquals("VIP", result.name)
        verify(ticketTierRepository).save(any())
    }

    @Test
    fun `addTierToEvent should fail if user is not organizer`() {
        setupMockUser(999L, listOf("events:ROLE_ORGANIZER"))
        val event = createEvent()
        val request = CreateTicketTierRequest(
            name = "VIP", price = BigDecimal("100"), totalAllocation = 50,
            validFrom = event.startDate, validUntil = event.endDate, createdBy = 1L
        )

        whenever(eventRepository.findById(100L)).thenReturn(Optional.of(event))

        assertThrows(UnauthorizedEventAccessException::class.java) {
            ticketTierService.addTierToEvent(100L, request, 999L)
        }
    }
}
