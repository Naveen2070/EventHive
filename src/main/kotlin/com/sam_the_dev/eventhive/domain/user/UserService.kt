package com.sam_the_dev.eventhive.domain.user

import com.sam_the_dev.eventhive.api.dto.ChangePasswordRequest
import com.sam_the_dev.eventhive.api.dto.RegisterUserDTO
import com.sam_the_dev.eventhive.api.dto.UpdateUserRequest
import com.sam_the_dev.eventhive.api.dto.UserDTO

interface UserService {
    fun registerUser(user: RegisterUserDTO): User
    fun getUserById(id: Long): UserDTO
    fun getUserByEmailOrUsername(uniqueId: String): User
    fun updateUser(userId: Long, request: UpdateUserRequest, currentUserEmail: String): UserDTO
    fun changePassword(userId: Long, request: ChangePasswordRequest, currentUserEmail: String)
    fun initiatePasswordReset(email: String)
    fun completePasswordReset(token: String, newPassword: String)
}