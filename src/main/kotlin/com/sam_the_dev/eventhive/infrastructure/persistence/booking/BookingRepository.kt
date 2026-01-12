package com.sam_the_dev.eventhive.infrastructure.persistence.booking

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BookingRepository : JpaRepository<BookingEntity, Long> {
    // Find all bookings made by a specific user
    fun findByUserId(userId: Long, pageable: Pageable): Page<BookingEntity>

    // Find all bookings for a specific event (for the Organizer to see who is coming)
    fun findByEventId(eventId: Long, pageable: Pageable): Page<BookingEntity>
}