package com.thehiveproject.event.domain.dashboard
import com.thehiveproject.event.api.dto.DashboardStatsDTO

interface DashboardService {
    fun getOrganizerStats(token: String): DashboardStatsDTO
}
