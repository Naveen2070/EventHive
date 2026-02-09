package com.sam_the_dev.eventhive.domain.user.error

class UnauthorizedUserAccessException(
    message: String = "Access Denied: You can only update your own profile"
) : RuntimeException(message)