package com.thehiveproject.event.domain.role.error

class RoleNotFoundException (
    message: String = "Role not found"
): RuntimeException(message)