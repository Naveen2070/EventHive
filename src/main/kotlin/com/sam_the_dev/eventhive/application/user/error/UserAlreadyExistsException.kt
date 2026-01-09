package com.sam_the_dev.eventhive.application.user.error

class UserAlreadyExistsException(message: String = "User already exists") : RuntimeException(message)