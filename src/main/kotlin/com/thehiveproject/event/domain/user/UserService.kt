package com.thehiveproject.event.domain.user

import com.thehiveproject.event.api.dto.ChangePasswordRequest
import com.thehiveproject.event.api.dto.RegisterUserDTO
import com.thehiveproject.event.api.dto.UpdateUserRequest
import com.thehiveproject.event.api.dto.UserDTO

interface UserService {
    fun registerUser(user: RegisterUserDTO): User
    fun getUserById(id: Long): UserDTO
    fun getUserByEmailOrUsername(uniqueId: String): User
    fun updateUser(userId: Long, request: UpdateUserRequest, currentUserEmail: String): UserDTO
    fun changePassword(userId: Long, request: ChangePasswordRequest, currentUserEmail: String)
    fun initiatePasswordReset(email: String)
    fun completePasswordReset(token: String, newPassword: String)
}