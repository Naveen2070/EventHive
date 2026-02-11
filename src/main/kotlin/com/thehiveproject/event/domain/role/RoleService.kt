package com.thehiveproject.event.domain.role

import com.thehiveproject.event.api.dto.UserDTO

interface RoleService {
    fun assignRoleToUser(userId: Long, roleName: String, updatedBy: Long): UserDTO
    fun removeRoleFromUser(userId: Long, roleName: String, updatedBy: Long): UserDTO
}