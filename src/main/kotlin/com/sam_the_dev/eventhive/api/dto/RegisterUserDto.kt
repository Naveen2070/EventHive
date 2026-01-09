package com.sam_the_dev.eventhive.api.dto

data class RegisterUserDto(
    val username: String,
    val email: String,
    val password: String,
)
