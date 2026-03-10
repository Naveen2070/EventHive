package com.thehiveproject.event.application.dashboard

import com.thehiveproject.event.api.dto.DashboardStatsDTO
import com.thehiveproject.event.api.dto.RecentSaleDTO
import com.thehiveproject.event.api.dto.RevenueTrendItem
import com.thehiveproject.event.domain.dashboard.DashboardService
import com.thehiveproject.event.infrastructure.persistence.booking.BookingRepository
import com.thehiveproject.event.infrastructure.persistence.booking.projection.RecentSaleProjection
import com.thehiveproject.event.infrastructure.persistence.booking.projection.RevenueTrendProjection
import com.thehiveproject.event.infrastructure.persistence.event.EventRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class DashboardServiceImpl(
    private val bookingRepository: BookingRepository,
    private val eventRepository: EventRepository,
) : DashboardService {

    @Transactional
    override fun getOrganizerStats(userId: Long): DashboardStatsDTO {

        val totalEvents = eventRepository.countByOrganizerId(userId)
        val activeEvents = eventRepository.countByOrganizerIdAndEndDateAfter(
            userId,
            LocalDateTime.now()
        )

        val totalRevenue = bookingRepository.getTotalRevenue(userId)
        val totalTicketsSold = bookingRepository.getTotalTicketsSold(userId)
        val pendingPaymentTickets = bookingRepository.getPendingPaymentTickets(userId)

        // Convert LocalDateTime to Instant
        val now = Instant.now()
        val oneWeekAgo = now.minusSeconds(7 * 24 * 60 * 60)
        val twoWeeksAgo = now.minusSeconds(14 * 24 * 60 * 60)
        val oneMonthAgo = now.minusSeconds(30 * 24 * 60 * 60)
        val twoMonthsAgo = now.minusSeconds(60 * 24 * 60 * 60)

        val ticketsSoldLastWeek = bookingRepository.getTicketsSoldSince(userId, oneWeekAgo)
        val revenueLastWeek = bookingRepository.getRevenueSince(userId, oneWeekAgo)
        val revenuePreviousWeek = bookingRepository.getRevenueSince(userId, twoWeeksAgo) - revenueLastWeek
        val revenueLastMonth = bookingRepository.getRevenueSince(userId, oneMonthAgo)
        val revenuePreviousMonth = bookingRepository.getRevenueSince(userId, twoMonthsAgo) - revenueLastMonth

        fun growthPercent(current: Double, previous: Double): Double =
            if (previous == 0.0) 0.0 else ((current - previous) / previous) * 100

        val revenueGrowthLastWeekPercent = growthPercent(revenueLastWeek, revenuePreviousWeek)
        val revenueGrowthLastMonthPercent = growthPercent(revenueLastMonth, revenuePreviousMonth)

        val revenueTrend: List<RevenueTrendItem> = bookingRepository.getRevenueTrendProjected(userId)
            .map { it: RevenueTrendProjection ->
                RevenueTrendItem(it.date, it.revenue)
            }

        val recentSales: List<RecentSaleDTO> = bookingRepository.findRecentSales(
            userId,
            PageRequest.of(0, 5)
        ).map { it: RecentSaleProjection ->
            RecentSaleDTO(
                it.id,
                it.eventName,
                it.userId.toString(),
                it.tierName,
                it.tickets,
                it.amount,
                it.date.atZone(ZoneId.systemDefault()).toLocalDateTime()
            )
        }

        return DashboardStatsDTO(
            totalRevenue,
            totalTicketsSold,
            pendingPaymentTickets,
            ticketsSoldLastWeek,
            activeEvents,
            totalEvents,
            revenueGrowthLastWeekPercent,
            revenueGrowthLastMonthPercent,
            revenueTrend,
            recentSales
        )
    }
}
