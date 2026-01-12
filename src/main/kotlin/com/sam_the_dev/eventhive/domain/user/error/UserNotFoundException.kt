package com.sam_the_dev.eventhive.domain.user.error

class UserNotFoundException(uniqueId: String, message: String = "User with following details not found") :
    RuntimeException("$message=$uniqueId")
