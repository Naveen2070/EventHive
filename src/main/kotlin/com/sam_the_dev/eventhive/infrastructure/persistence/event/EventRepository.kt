package com.sam_the_dev.eventhive.infrastructure.persistence.event

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventRepository : JpaRepository<EventEntity, Long>{
    fun findByOrganizerId(organizerId: Long, pageable: Pageable): Page<EventEntity>
}