package com.sam_the_dev.eventhive.domain.role

import com.sam_the_dev.eventhive.api.dto.UserDTO

interface RoleService {
    fun assignRoleToUser(userId: Long, roleName: String, updatedBy: Long): UserDTO
    fun removeRoleFromUser(userId: Long, roleName: String, updatedBy: Long): UserDTO
}