package com.sam_the_dev.eventhive.domain.user

import com.sam_the_dev.eventhive.api.dto.RegisterUserDTO
import com.sam_the_dev.eventhive.api.dto.UserDTO

interface UserService {
    fun registerUser(user: RegisterUserDTO): UserDTO
    fun getUserById(id: Long): UserDTO
    fun getUserByEmailOrUsername(uniqueId: String): User
}