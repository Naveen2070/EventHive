package com.thehiveproject.event.infrastructure.persistence.booking

import com.thehiveproject.event.infrastructure.persistence.booking.projection.RecentSaleProjection
import com.thehiveproject.event.infrastructure.persistence.booking.projection.RevenueTrendProjection
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

    // --- Dashboard Queries ---

    // 1. Recent Sales
    @Query("""
        SELECT 
            b.id AS id,
            e.title AS eventName,
            t.name AS tierName,  
            u.username AS customerName,
            b.ticketsCount AS tickets,
            b.totalPrice AS amount,
            b.createdAt AS date
        FROM BookingEntity b
        JOIN b.event e
        JOIN b.ticketTier t      
        JOIN b.user u
        WHERE e.organizer.id = :organizerId
        AND b.status = com.thehiveproject.event.domain.booking.BookingStatus.CONFIRMED
        ORDER BY b.createdAt DESC
    """)
    fun findRecentSales(
        organizerId: Long,
        pageable: Pageable
    ): List<RecentSaleProjection>

    // 2. Revenue Trend
    @Query("""
        SELECT 
            CAST(b.createdAt AS LocalDate) AS date, 
            COALESCE(SUM(b.totalPrice), 0) AS revenue
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.thehiveproject.event.domain.booking.BookingStatus.CONFIRMED
        GROUP BY CAST(b.createdAt AS LocalDate)
        ORDER BY CAST(b.createdAt AS LocalDate) ASC
    """)
    fun getRevenueTrendProjected(
        organizerId: Long
    ): List<RevenueTrendProjection>

    // 3. Total Revenue
    @Query("""
        SELECT COALESCE(SUM(b.totalPrice), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.thehiveproject.event.domain.booking.BookingStatus.CONFIRMED
    """)
    fun getTotalRevenue(organizerId: Long): Double

    // 4. Total Tickets Sold (CONFIRMED + PENDING)
    @Query("""
        SELECT COALESCE(SUM(b.ticketsCount), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status IN (
            com.thehiveproject.event.domain.booking.BookingStatus.CONFIRMED,
            com.thehiveproject.event.domain.booking.BookingStatus.PENDING_PAYMENT
        )
    """)
    fun getTotalTicketsSold(organizerId: Long): Long

    // 5. Pending Payment Count
    @Query("""
        SELECT COALESCE(SUM(b.ticketsCount), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.thehiveproject.event.domain.booking.BookingStatus.PENDING_PAYMENT
    """)
    fun getPendingPaymentTickets(organizerId: Long): Long

    // 6. Tickets Sold Since (Date Filter)
    @Query("""
        SELECT COALESCE(SUM(b.ticketsCount), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.thehiveproject.event.domain.booking.BookingStatus.CONFIRMED
        AND b.createdAt >= :fromDate
    """)
    fun getTicketsSoldSince(
        organizerId: Long,
        fromDate: Instant
    ): Long

    // 7. Revenue Since (Date Filter)
    @Query("""
        SELECT COALESCE(SUM(b.totalPrice), 0)
        FROM BookingEntity b
        JOIN b.event e
        WHERE e.organizer.id = :organizerId
        AND b.status = com.thehiveproject.event.domain.booking.BookingStatus.CONFIRMED
        AND b.createdAt >= :fromDate
    """)
    fun getRevenueSince(
        organizerId: Long,
        fromDate: Instant
    ): Double
}