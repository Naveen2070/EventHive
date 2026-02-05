package com.sam_the_dev.eventhive.domain.event

import com.sam_the_dev.eventhive.api.dto.CreateTicketTierRequest
import com.sam_the_dev.eventhive.api.dto.TicketTierDTO
import com.sam_the_dev.eventhive.api.dto.UpdateTicketTierRequest

interface TicketTierService {
    fun addTierToEvent(
        eventId: Long,
        request: CreateTicketTierRequest,
        userEmail: String,
        isAdmin: Boolean
    ): TicketTier

    fun updateTier(tierId: Long, request: UpdateTicketTierRequest, userEmail: String, isAdmin: Boolean): TicketTier
    fun deleteTier(tierId: Long, userEmail: String, isAdmin: Boolean)
    fun getTierById(tierId: Long): TicketTierDTO
}