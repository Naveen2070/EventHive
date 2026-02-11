package com.thehiveproject.event.domain.user.error

class UserNotFoundException(uniqueId: String, message: String = "User with following details not found") :
    RuntimeException("$message=$uniqueId")
