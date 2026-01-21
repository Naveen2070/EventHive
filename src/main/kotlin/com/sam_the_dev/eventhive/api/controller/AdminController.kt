package com.sam_the_dev.eventhive.api.controller

import com.sam_the_dev.eventhive.api.dto.RoleAssignmentRequest
import com.sam_the_dev.eventhive.api.dto.UserDTO
import com.sam_the_dev.eventhive.domain.role.RoleService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val roleService: RoleService
) {

    @PutMapping("/roles/assign/{userId}")
    fun assignRole(
        @PathVariable userId: Long,
        @Valid @RequestBody request: RoleAssignmentRequest
    ): ResponseEntity<UserDTO> {
        val updatedUser = roleService.assignRoleToUser(userId, request.roleName,request.updateBy)
        return ResponseEntity.ok(updatedUser)
    }

    @DeleteMapping("/roles/remove/{userId}")
    fun removeRole(
        @PathVariable userId: Long,
        @Valid  @RequestBody request: RoleAssignmentRequest
    ): ResponseEntity<UserDTO> {
        val updatedUser = roleService.removeRoleFromUser(userId, request.roleName, request.updateBy)
        return ResponseEntity.ok(updatedUser)
    }
}