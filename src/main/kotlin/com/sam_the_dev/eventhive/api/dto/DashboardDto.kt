package com.sam_the_dev.eventhive.api.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class DashboardStatsDTO(
    val totalRevenue: Double,

    // Tickets
    val totalTicketsSold: Long,
    val pendingPaymentTickets: Long,
    val ticketsSoldLastWeek: Long,

    // Events
    val activeEvents: Long,
    val totalEvents: Long,

    // Revenue comparison
    val revenueGrowthLastWeekPercent: Double,
    val revenueGrowthLastMonthPercent: Double,

    val revenueTrend: List<RevenueTrendItem>,
    val recentSales: List<RecentSaleDTO>
)

data class RevenueTrendItem(
    val date: LocalDate,
    val revenue: Double
)

data class RecentSaleDTO(
    val id: Long,
    val eventName: String,
    val customerName: String,
    val tickets: Int,
    val amount: Double,
    val date: LocalDateTime
)