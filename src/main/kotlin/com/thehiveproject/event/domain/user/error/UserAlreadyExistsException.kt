package com.thehiveproject.event.domain.user.error

class UserAlreadyExistsException(message: String = "User already exists") : RuntimeException(message)