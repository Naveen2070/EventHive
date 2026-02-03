package com.sam_the_dev.eventhive.api.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class DashboardStatsDTO(
    val totalRevenue: Double,
    val totalTicketsSold: Long,
    val activeEvents: Long,
    val totalEvents: Long,
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