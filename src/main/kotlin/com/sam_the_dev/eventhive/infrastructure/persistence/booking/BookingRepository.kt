package com.sam_the_dev.eventhive.infrastructure.persistence.booking

import com.sam_the_dev.eventhive.infrastructure.persistence.booking.projection.RecentSaleProjection
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.projection.RevenueTrendProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface BookingRepository : JpaRepository<BookingEntity, Long> {

    fun findByUserId(userId: Long, pageable: Pageable): Page<BookingEntity>
    fun findByEventId(eventId: Long, pageable: Pageable): Page<BookingEntity>
    fun findByBookingReference(bookingReference: String): BookingEntity?

    // Confirmed bookings for organizer
    @Query("""
        SELECT b FROM BookingEntity b 
        JOIN b.event e 
        WHERE e.organizer.id = :organizerId 
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED
    """)
    fun findAllConfirmedByOrganizer(organizerId: Long): List<BookingEntity>

    // --- Projections for Dashboard ---

    @Query("""
        SELECT 
            b.id AS id,
            e.title AS eventName,
            u.username AS customerName,
            b.ticketsCount AS tickets,
            b.totalPrice AS amount,
            b.createdAt AS date
        FROM BookingEntity b
        JOIN b.event e
        JOIN b.user u
        WHERE e.organizer.id = :organizerId
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED
        ORDER BY b.createdAt DESC
    """)
    fun findRecentSales(
        organizerId: Long,
        pageable: Pageable
    ): List<RecentSaleProjection>

    @Query("""
        SELECT 
            DATE(b.createdAt) AS date,
            COALESCE(SUM(b.totalPrice), 0) AS revenue
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED
        GROUP BY DATE(b.createdAt)
        ORDER BY DATE(b.createdAt)
    """)
    fun getRevenueTrendProjected(
        organizerId: Long
    ): List<RevenueTrendProjection>

    // Total revenue
    @Query("""
        SELECT COALESCE(SUM(b.totalPrice), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED
    """)
    fun getTotalRevenue(organizerId: Long): Double

    // Total tickets
    @Query("""
        SELECT COALESCE(SUM(b.ticketsCount), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status IN (
            com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED,
            com.sam_the_dev.eventhive.domain.booking.BookingStatus.PENDING_PAYMENT
        )
    """)
    fun getTotalTicketsSold(organizerId: Long): Long

    // Pending payment tickets
    @Query("""
        SELECT COALESCE(SUM(b.ticketsCount), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.PENDING_PAYMENT
    """)
    fun getPendingPaymentTickets(organizerId: Long): Long

    // Tickets sold since a date
    @Query("""
        SELECT COALESCE(SUM(b.ticketsCount), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED
        AND b.createdAt >= :fromDate
    """)
    fun getTicketsSoldSince(
        organizerId: Long,
        fromDate: Instant
    ): Long

    // Revenue since a date
    @Query("""
        SELECT COALESCE(SUM(b.totalPrice), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.sam_the_dev.eventhive.domain.booking.BookingStatus.CONFIRMED
        AND b.createdAt >= :fromDate
    """)
    fun getRevenueSince(
        organizerId: Long,
        fromDate: Instant
    ): Double
}
