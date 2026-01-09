package com.sam_the_dev.eventhive.domain.user

import com.sam_the_dev.eventhive.api.dto.RegisterUserDto
import com.sam_the_dev.eventhive.api.dto.UserDTO

interface UserService {
    fun registerUser(user: RegisterUserDto): UserDTO
    fun getUserById(id: Long): UserDTO
    fun getUserByEmailOrUsername(uniqueId: String): User
}