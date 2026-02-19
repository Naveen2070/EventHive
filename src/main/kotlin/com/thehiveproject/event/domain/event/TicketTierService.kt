package com.thehiveproject.event.domain.event

import com.thehiveproject.event.api.dto.CreateTicketTierRequest
import com.thehiveproject.event.api.dto.UpdateTicketTierRequest

interface TicketTierService {
    fun addTierToEvent(
        eventId: Long,
        request: CreateTicketTierRequest,
        token: String
    ): TicketTier

    fun updateTier(tierId: Long, request: UpdateTicketTierRequest, token: String): TicketTier
    fun deleteTier(tierId: Long, token: String)
    fun getTierById(tierId: Long): TicketTier
}