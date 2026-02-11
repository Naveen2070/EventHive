package com.thehiveproject.event.infrastructure.persistence.event

import org.springframework.data.jpa.repository.JpaRepository

interface TicketTierRepository : JpaRepository<TicketTierEntity, Long>