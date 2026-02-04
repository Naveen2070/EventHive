package com.sam_the_dev.eventhive.application.dashboard

import com.sam_the_dev.eventhive.api.dto.DashboardStatsDTO
import com.sam_the_dev.eventhive.api.dto.RecentSaleDTO
import com.sam_the_dev.eventhive.api.dto.RevenueTrendItem
import com.sam_the_dev.eventhive.domain.dashboard.DashboardService
import com.sam_the_dev.eventhive.domain.user.error.UserNotFoundException
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.BookingRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.projection.RecentSaleProjection
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.projection.RevenueTrendProjection
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
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
    private val userRepository: UserRepository
) : DashboardService {

    @Transactional
    override fun getOrganizerStats(organizerName: String): DashboardStatsDTO {
        val organizer = userRepository.findByUsernameOrEmail(organizerName, organizerName)
            ?: throw UserNotFoundException("Organizer not found")
        val organizerId = organizer.id!!

        val totalEvents = eventRepository.countByOrganizerId(organizerId)
        val activeEvents = eventRepository.countByOrganizerIdAndEndDateAfter(
            organizerId,
            LocalDateTime.now()
        )

        val totalRevenue = bookingRepository.getTotalRevenue(organizerId)
        val totalTicketsSold = bookingRepository.getTotalTicketsSold(organizerId)
        val pendingPaymentTickets = bookingRepository.getPendingPaymentTickets(organizerId)

        // Convert LocalDateTime to Instant
        val now = Instant.now()
        val oneWeekAgo = now.minusSeconds(7 * 24 * 60 * 60)
        val twoWeeksAgo = now.minusSeconds(14 * 24 * 60 * 60)
        val oneMonthAgo = now.minusSeconds(30 * 24 * 60 * 60)
        val twoMonthsAgo = now.minusSeconds(60 * 24 * 60 * 60)

        val ticketsSoldLastWeek = bookingRepository.getTicketsSoldSince(organizerId, oneWeekAgo)
        val revenueLastWeek = bookingRepository.getRevenueSince(organizerId, oneWeekAgo)
        val revenuePreviousWeek = bookingRepository.getRevenueSince(organizerId, twoWeeksAgo) - revenueLastWeek
        val revenueLastMonth = bookingRepository.getRevenueSince(organizerId, oneMonthAgo)
        val revenuePreviousMonth = bookingRepository.getRevenueSince(organizerId, twoMonthsAgo) - revenueLastMonth

        fun growthPercent(current: Double, previous: Double): Double =
            if (previous == 0.0) 0.0 else ((current - previous) / previous) * 100

        val revenueGrowthLastWeekPercent = growthPercent(revenueLastWeek, revenuePreviousWeek)
        val revenueGrowthLastMonthPercent = growthPercent(revenueLastMonth, revenuePreviousMonth)

        val revenueTrend: List<RevenueTrendItem> = bookingRepository.getRevenueTrendProjected(organizerId)
            .map { it: RevenueTrendProjection ->
                RevenueTrendItem(it.date, it.revenue)
            }

        val recentSales: List<RecentSaleDTO> = bookingRepository.findRecentSales(
            organizerId,
            PageRequest.of(0, 5)
        ).map { it: RecentSaleProjection ->
            RecentSaleDTO(
                it.id,
                it.eventName,
                it.customerName,
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
