package com.sam_the_dev.eventhive.api.dto

data class RoleAssignmentRequest(
    val roleName: String,
    val updateBy: Long
)