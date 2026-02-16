package com.thehiveproject.event.application.event

import com.thehiveproject.event.api.dto.CreateTicketTierRequest
import com.thehiveproject.event.api.dto.UpdateTicketTierRequest
import com.thehiveproject.event.domain.event.TicketTier
import com.thehiveproject.event.domain.event.TicketTierService
import com.thehiveproject.event.domain.event.error.EventNotFoundException
import com.thehiveproject.event.domain.event.error.InvalidTicketTierException
import com.thehiveproject.event.domain.event.error.TicketTierNotFoundException
import com.thehiveproject.event.domain.event.error.UnauthorizedEventAccessException
import com.thehiveproject.event.infrastructure.persistence.event.*
import com.thehiveproject.event.infrastructure.security.JwtService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TicketTierServiceImpl(
    private val ticketTierRepository: TicketTierRepository,
    private val eventRepository: EventRepository,
    private val jwtService: JwtService
) : TicketTierService {

    @Transactional
    override fun addTierToEvent(
        eventId: Long,
        request: CreateTicketTierRequest,
        token: String
    ): TicketTier {
        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException("Event not found") }

        val userId = jwtService.extractUserId(token)
        val isAdmin = jwtService.hasAnyRole(token, "SUPER_ADMIN", "ADMIN")
        validateOwnership(event, userId, isAdmin)

        // 1. Validate Dates
        if (request.validFrom.isBefore(event.startDate) || request.validUntil.isAfter(event.endDate)) {
            throw InvalidTicketTierException("Tier validity must be within the Event's start and end dates.")
        }

        // 2. Validate Uniqueness
        if (event.ticketTiers.any { it.name.equals(request.name, ignoreCase = true) }) {
            throw InvalidTicketTierException("A ticket tier with the name '${request.name}' already exists.")
        }

        // 3. Create & Save
        val newTier = TicketTierEntity(
            name = request.name,
            price = request.price,
            totalAllocation = request.totalAllocation,
            availableAllocation = request.totalAllocation,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            event = event,
            createdBy = request.createdBy,
            updatedBy = request.createdBy,
        )

        val savedTier = ticketTierRepository.save(newTier)
        return savedTier.toDomain()
    }

    @Transactional
    override fun updateTier(
        tierId: Long,
        request: UpdateTicketTierRequest,
        token: String,
    ): TicketTier {
        val tier = ticketTierRepository.findById(tierId)
            .orElseThrow { TicketTierNotFoundException("Ticket tier not found") }

        val isAdmin = jwtService.hasAnyRole(token, "SUPER_ADMIN", "ADMIN")
        val userId = jwtService.extractUserId(token)
        validateOwnership(tier.event, userId, isAdmin)

        // 1. Validate Updates
        request.name?.let { newName ->
            // Check duplicates only if name actually changed
            if (newName != tier.name && tier.event.ticketTiers.any { it.name.equals(newName, ignoreCase = true) }) {
                throw InvalidTicketTierException("A ticket tier with the name '$newName' already exists.")
            }
            tier.name = newName
        }

        request.price?.let { tier.price = it}

        // 2. 🛡️ Complex Logic: Updating Allocation
        request.totalAllocation?.let { newTotal ->
            val soldCount = tier.totalAllocation - tier.availableAllocation

            if (newTotal < soldCount) {
                throw InvalidTicketTierException(
                    "Cannot reduce total allocation to $newTotal because $soldCount tickets are already sold."
                )
            }

            // Recalculate available: New Total - Sold
            tier.totalAllocation = newTotal
            tier.availableAllocation = newTotal - soldCount
        }

        // 3. Validate Dates
        if (request.validFrom != null || request.validUntil != null) {
            val newStart = request.validFrom ?: tier.validFrom
            val newEnd = request.validUntil ?: tier.validUntil

            if (newStart.isBefore(tier.event.startDate) || newEnd.isAfter(tier.event.endDate)) {
                throw InvalidTicketTierException("Tier validity must be within the Event's dates.")
            }
            tier.validFrom = newStart
            tier.validUntil = newEnd
        }

        tier.updatedBy = request.updatedBy

        val savedTier = ticketTierRepository.save(tier)
        return savedTier.toDomain()
    }

    @Transactional
    override fun deleteTier(tierId: Long, token: String) {
        val tier = ticketTierRepository.findById(tierId)
            .orElseThrow { TicketTierNotFoundException("Ticket tier not found") }

        val userId = jwtService.extractUserId(token)
        val isAdmin = jwtService.hasAnyRole(token, "SUPER_ADMIN", "ADMIN")
        validateOwnership(tier.event, userId, isAdmin)

        // 🛡️ CRITICAL: Do not delete if tickets sold
        if (tier.totalAllocation != tier.availableAllocation) {
            throw InvalidTicketTierException(
                "Cannot delete this tier because tickets have already been sold. You should disable it or reduce allocation instead."
            )
        }

        // Hard Delete is safe because no bookings exist
        ticketTierRepository.delete(tier)
    }

    private fun validateOwnership(event: EventEntity, userId: Long, isAdmin: Boolean) {
        if (isAdmin) return

        if (event.organizerId != userId ) {
            throw UnauthorizedEventAccessException("Access Denied: You are not the organizer of this event.")
        }
    }


    @Transactional(readOnly = true)
    override fun getTierById(tierId: Long): TicketTier {
        val tier = ticketTierRepository.findById(tierId)
            .orElseThrow { TicketTierNotFoundException("Ticket tier not found with ID: $tierId") }
        return tier.toDomain()
    }
}