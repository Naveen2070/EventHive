package com.sam_the_dev.eventhive.domain.role.error

class RoleNotFoundException (
    message: String = "Role not found"
): RuntimeException(message)