package com.thehiveproject.event.domain.event

import com.thehiveproject.event.api.dto.CreateTicketTierRequest
import com.thehiveproject.event.api.dto.UpdateTicketTierRequest

interface TicketTierService {
    fun addTierToEvent(
        eventId: Long,
        request: CreateTicketTierRequest,
        userEmail: String,
        isAdmin: Boolean
    ): TicketTier

    fun updateTier(tierId: Long, request: UpdateTicketTierRequest, userEmail: String, isAdmin: Boolean): TicketTier
    fun deleteTier(tierId: Long, userEmail: String, isAdmin: Boolean)
    fun getTierById(tierId: Long): TicketTier
}