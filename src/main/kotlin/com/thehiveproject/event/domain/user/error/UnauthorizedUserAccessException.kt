package com.thehiveproject.event.domain.user.error

class UnauthorizedUserAccessException(
    message: String = "Access Denied: You can only update your own profile"
) : RuntimeException(message)