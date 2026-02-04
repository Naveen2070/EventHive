package com.sam_the_dev.eventhive.application.user

import com.sam_the_dev.eventhive.api.dto.RegisterUserDTO
import com.sam_the_dev.eventhive.api.dto.UserDTO
import com.sam_the_dev.eventhive.api.mapper.toDTO
import com.sam_the_dev.eventhive.domain.user.User
import com.sam_the_dev.eventhive.domain.user.UserService
import com.sam_the_dev.eventhive.domain.user.error.UserAlreadyExistsException
import com.sam_the_dev.eventhive.domain.user.error.UserNotFoundException
import com.sam_the_dev.eventhive.infrastructure.persistence.role.RoleRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.role.UserRoleEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import com.sam_the_dev.eventhive.infrastructure.persistence.user.toDomain
import com.sam_the_dev.eventhive.infrastructure.persistence.user.toEntity
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val roleRepository: RoleRepository
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
}