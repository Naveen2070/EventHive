package com.sam_the_dev.eventhive.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class RoleAssignmentRequest(
    @field:NotBlank(message = "Role name is required")
    val roleName: String,

    @field:NotNull(message = "Updated by user ID is required")
    @field:Positive(message = "Updated by user ID must be positive")
    var updateBy: Long
)
