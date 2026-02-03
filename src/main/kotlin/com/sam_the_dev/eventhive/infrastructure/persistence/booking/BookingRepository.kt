package com.sam_the_dev.eventhive.infrastructure.persistence.booking

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BookingRepository : JpaRepository<BookingEntity, Long> {
    // Find all bookings made by a specific user
    fun findByUserId(userId: Long, pageable: Pageable): Page<BookingEntity>

    // Find all bookings for a specific event (for the Organizer to see who is coming)
    fun findByEventId(eventId: Long, pageable: Pageable): Page<BookingEntity>

    // Find by Booking Reference
    fun findByBookingReference(bookingReference: String): BookingEntity?

    // Confirmed bookings for organizer
    @Query(
        """
        SELECT b FROM BookingEntity b 
        JOIN b.event e 
        WHERE e.organizer.id = :organizerId 
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED
    """
    )
    fun findAllConfirmedByOrganizer(organizerId: Long): List<BookingEntity>

    // Recent bookings
    @Query(
        """
        SELECT b FROM BookingEntity b 
        JOIN b.event e 
        WHERE e.organizer.id = :organizerId 
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED
        ORDER BY b.createdAt DESC
    """
    )
    fun findRecentBookings(
        organizerId: Long,
        pageable: Pageable
    ): List<BookingEntity>

    // Total Revenue
    @Query(
        """
        SELECT COALESCE(SUM(b.totalPrice), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED
    """
    )
    fun getTotalRevenue(organizerId: Long): Double

    // Total Tickets Sold
    @Query(
        """
        SELECT COALESCE(SUM(b.ticketsCount), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED
    """
    )
    fun getTotalTicketsSold(organizerId: Long): Long

    // Revenue Trend
    @Query(
        """
        SELECT DATE(b.createdAt), COALESCE(SUM(b.totalPrice), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED
        GROUP BY DATE(b.createdAt)
        ORDER BY DATE(b.createdAt)
    """
    )
    fun getRevenueTrend(
        organizerId: Long
    ): List<Array<Any>>
}