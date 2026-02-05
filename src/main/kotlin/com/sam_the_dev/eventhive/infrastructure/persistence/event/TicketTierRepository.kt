package com.sam_the_dev.eventhive.infrastructure.persistence.event

import org.springframework.data.jpa.repository.JpaRepository

interface TicketTierRepository : JpaRepository<TicketTierEntity, Long>