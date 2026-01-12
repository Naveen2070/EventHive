package com.sam_the_dev.eventhive.domain.user.error

class UserAlreadyExistsException(message: String = "User already exists") : RuntimeException(message)