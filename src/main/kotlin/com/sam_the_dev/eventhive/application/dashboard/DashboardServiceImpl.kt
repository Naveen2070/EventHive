package com.sam_the_dev.eventhive.application.dashboard

import com.sam_the_dev.eventhive.api.dto.DashboardStatsDTO
import com.sam_the_dev.eventhive.api.dto.RecentSaleDTO
import com.sam_the_dev.eventhive.api.dto.RevenueTrendItem
import com.sam_the_dev.eventhive.domain.dashboard.DashboardService
import com.sam_the_dev.eventhive.domain.user.error.UserNotFoundException
import com.sam_the_dev.eventhive.infrastructure.persistence.booking.BookingRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class DashboardServiceImpl(
    private val bookingRepository: BookingRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
): DashboardService {

    @Transactional
    override fun getOrganizerStats(organizerName: String): DashboardStatsDTO {
        val organizer = userRepository.findByUsernameOrEmail(organizerName, organizerName)
            ?: throw UserNotFoundException("Organizer not found")

        val organizerId = organizer.id!!

        val totalEvents = eventRepository.countByOrganizerId(organizerId)
        val activeEvents =
            eventRepository.countByOrganizerIdAndEndDateAfter(
                organizerId,
                LocalDateTime.now()
            )

        val totalRevenue = bookingRepository.getTotalRevenue(organizerId)
        val totalTicketsSold = bookingRepository.getTotalTicketsSold(organizerId)

        val revenueTrend = bookingRepository.getRevenueTrend(organizerId)
            .map { row ->
                RevenueTrendItem(
                    date = (row[0] as java.sql.Date).toLocalDate(),
                    revenue = (row[1] as Number).toDouble()
                )
            }


        val recentSales = bookingRepository
            .findRecentBookings(
                organizerId,
                PageRequest.of(0, 5)
            )
            .map { booking ->
                RecentSaleDTO(
                    id = booking.id!!,
                    eventName = booking.event.title,
                    customerName = booking.user.username,
                    tickets = booking.ticketsCount,
                    amount = booking.totalPrice.toDouble(),
                    date = booking.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime(),
                )
            }

        return DashboardStatsDTO(
            totalRevenue = totalRevenue,
            totalTicketsSold = totalTicketsSold,
            activeEvents = activeEvents,
            totalEvents = totalEvents,
            revenueTrend = revenueTrend,
            recentSales = recentSales
        )
    }
}