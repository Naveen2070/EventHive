package com.thehiveproject.event.domain.event

import com.thehiveproject.event.api.dto.CreateTicketTierRequest
import com.thehiveproject.event.api.dto.UpdateTicketTierRequest

interface TicketTierService {
    fun addTierToEvent(
        eventId: Long,
        request: CreateTicketTierRequest,
        userId: Long
    ): TicketTier

    fun updateTier(tierId: Long, request: UpdateTicketTierRequest, userId: Long): TicketTier
    fun deleteTier(tierId: Long, userId: Long)
    fun getTierById(tierId: Long): TicketTier
}