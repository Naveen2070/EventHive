package com.sam_the_dev.eventhive.domain.dashboard
import com.sam_the_dev.eventhive.api.dto.DashboardStatsDTO

interface DashboardService {
    fun getOrganizerStats(organizerName: String): DashboardStatsDTO
}
