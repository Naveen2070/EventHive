package com.thehiveproject.event.application.user

import com.thehiveproject.event.api.dto.ChangePasswordRequest
import com.thehiveproject.event.api.dto.RegisterUserDTO
import com.thehiveproject.event.api.dto.UpdateUserRequest
import com.thehiveproject.event.api.dto.UserDTO
import com.thehiveproject.event.api.mapper.toDTO
import com.thehiveproject.event.domain.user.User
import com.thehiveproject.event.domain.user.UserService
import com.thehiveproject.event.domain.user.error.*
import com.thehiveproject.event.domain.user.event.PasswordChangedEvent
import com.thehiveproject.event.domain.user.event.PasswordResetInitiatedEvent
import com.thehiveproject.event.infrastructure.persistence.role.RoleRepository
import com.thehiveproject.event.infrastructure.persistence.role.UserRoleEntity
import com.thehiveproject.event.infrastructure.persistence.user.*
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val roleRepository: RoleRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val eventPublisher: ApplicationEventPublisher
) : UserService {
    private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    @Transactional
    override fun registerUser(user: RegisterUserDTO): User {
        // Check if user with username or email already exists
        val existingUser = userRepository.findByUsernameOrEmail(user.username, user.email)
        if (existingUser != null) {
            throw UserAlreadyExistsException("User with username or email already exists")
        }

        val userRole = roleRepository.findByName(user.role)

        if (userRole == null) {
            logger.error("Role ${user.role} not found in database")
            throw RuntimeException("Role ${user.role} not found in database")
        }

        // Hash the password
        val hashedPassword = passwordEncoder.encode(user.password)
        // Create a new user
        val newUser = User(
            id = null,
            username = user.username,
            email = user.email,
            password = hashedPassword,
            createdBy = 0L,
            updatedBy = 0L
        )

        val newUserEntity = newUser.toEntity()

        val roleMapping = UserRoleEntity(
            user = newUserEntity,
            role = userRole,
            createdBy = 0L,
            updatedBy = 0L
        )

        newUserEntity.userRoles.add(roleMapping)

        try {
            // Save the user to the database and return the saved user
            val savedUser = userRepository.save(newUserEntity)
            logger.info("User registered successfully: ${savedUser.username}")
            return savedUser.toDomain()
        } catch (e: Exception) {
            logger.error("Error registering user: ${e.message}")
            throw Exception("Failed to register user", e)
        }
    }

    @Transactional(readOnly = true)
    override fun getUserById(id: Long): UserDTO {
        return try {
            userRepository.findById(id)
                .orElseThrow { UserNotFoundException(id.toString(), "User not found with id=$id") }
                .toDomain()
                .toDTO()
        } catch (e: RuntimeException) {
            logger.error("Error getting user by id=$id", e)
            throw e
        }
    }


    @Transactional(readOnly = true)
    override fun getUserByEmailOrUsername(uniqueId: String): User {
        try {
            val user = userRepository.findByUsernameOrEmail(uniqueId, uniqueId)
                ?: throw UserNotFoundException(uniqueId, "User not found with credentials =$uniqueId")
            return user.toDomain()
        } catch (e: RuntimeException) {
            logger.error("Error getting user by credentials=$uniqueId", e)
            throw e
        }
    }

    @Transactional
    override fun updateUser(userId: Long, request: UpdateUserRequest, currentUserEmail: String): UserDTO {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId.toString(), "User not found") }

        // 1. Security Check: Can only update own profile
        if (userEntity.email != currentUserEmail && userEntity.username != currentUserEmail) {
            throw UnauthorizedUserAccessException("Access Denied: You can only update your own profile")
        }

        // 2. Update Fields
        request.username?.let {
            // Check uniqueness if username changed
            if (it != userEntity.username && userRepository.existsByUsername(it)) {
                throw UserAlreadyExistsException("Username '$it' is already taken")
            }
            userEntity.username = it
        }

        // Save
        val savedUser = userRepository.save(userEntity)
        return savedUser.toDomain().toDTO()
    }

    @Transactional
    override fun changePassword(userId: Long, request: ChangePasswordRequest, currentUserEmail: String) {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId.toString(), "User not found") }

        // 1. Security Check
        if (userEntity.email != currentUserEmail && userEntity.username != currentUserEmail) {
            throw UnauthorizedUserAccessException("Access Denied: You can only update your own password")
        }

        // 2. Verify Old Password
        if (!passwordEncoder.matches(request.oldPassword, userEntity.password)) {
            throw InvalidOldPasswordException("Invalid old password")
        }

        // 3. Update Password
        userEntity.password = passwordEncoder.encode(request.newPassword)
        userRepository.save(userEntity)
        eventPublisher.publishEvent(
            PasswordChangedEvent(email = userEntity.email, username = userEntity.username)
        )
        logger.info("Password changed successfully for user: ${userEntity.username}")
    }

    @Transactional
    override fun initiatePasswordReset(email: String) {
        // 1. Find User
        val userEntity =
            userRepository.findByEmail(email) ?: throw UserNotFoundException(email,"No user found with email: $email")

        // 2. Clean up old tokens
        passwordResetTokenRepository.deleteByUser_Id(userEntity.id!!)

        // 3. Generate Token
        val token = UUID.randomUUID().toString()
        val resetToken = PasswordResetTokenEntity(
            token = token,
            user = userEntity,
            expiryDate = LocalDateTime.now().plusHours(2) // 2 hour expiry
        )
        passwordResetTokenRepository.save(resetToken)

        logger.info("Password reset initiated for $email")

        eventPublisher.publishEvent(
            PasswordResetInitiatedEvent(
                email = userEntity.email,
                token = token,
                username = userEntity.username
            )
        )
    }

    @Transactional
    override fun completePasswordReset(token: String, newPassword: String) {
        // 1. Find Token
        val resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow { InvalidResetTokenException("Invalid or expired password reset token") }

        // 2. Check Expiry
        if (resetToken.expiryDate.isBefore(LocalDateTime.now())) {
            logger.info("Token has expired for user: ${resetToken.user.username}")
            // Delete expired token (Single use)
            passwordResetTokenRepository.delete(resetToken)
            throw ExpiredResetTokenException("Token has expired")
        }

        // 3. Update Password
        val user = resetToken.user
        user.password = passwordEncoder.encode(newPassword)
        userRepository.save(user)

        // 4. Delete Token (Single use)
        passwordResetTokenRepository.delete(resetToken)

        logger.info("Password successfully reset for user: ${user.username}")

        eventPublisher.publishEvent(
            PasswordChangedEvent(email = user.email, username = user.username)
        )
    }
}