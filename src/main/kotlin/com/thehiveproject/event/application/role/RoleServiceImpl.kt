package com.thehiveproject.event.application.role

import com.thehiveproject.event.api.dto.UserDTO
import com.thehiveproject.event.api.mapper.toDTO
import com.thehiveproject.event.domain.role.error.RoleNotFoundException
import com.thehiveproject.event.domain.user.error.UserNotFoundException
import com.thehiveproject.event.domain.role.RoleService
import com.thehiveproject.event.infrastructure.persistence.role.RoleRepository
import com.thehiveproject.event.infrastructure.persistence.role.UserRoleEntity
import com.thehiveproject.event.infrastructure.persistence.user.UserRepository
import com.thehiveproject.event.infrastructure.persistence.user.toDomain
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoleServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) : RoleService {

    private val logger = LoggerFactory.getLogger(RoleServiceImpl::class.java)

    @Transactional
    override fun assignRoleToUser(userId: Long, roleName: String, updatedBy: Long): UserDTO {
        // 1. Find User
        val userEntity = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId.toString(), "User not found") }

        // 2. Find Role
        val roleEntity = roleRepository.findByName(roleName)
            ?: throw RoleNotFoundException("Role '$roleName' not found")

        // 3. Check if user already has this role to prevent duplicates
        val alreadyHasRole = userEntity.userRoles.any { it.role.name == roleName }
        if (alreadyHasRole) {
            logger.info("User ${userEntity.username} already has role $roleName")
            return userEntity.toDomain().toDTO()
        }

        // 4. Create the new Relationship Entity
        val newRoleMapping = UserRoleEntity(
            user = userEntity,
            role = roleEntity,
            createdBy = roleEntity.createdBy,
            updatedBy = updatedBy
        )

        // 5. Add to User and Save
        // Because of CascadeType.ALL on UserEntity, saving the user saves the new role mapping
        userEntity.userRoles.add(newRoleMapping)
        val savedUser = userRepository.save(userEntity)

        logger.info("Assigned role $roleName to user ${userEntity.username}")
        return savedUser.toDomain().toDTO()
    }

    @Transactional
    override fun removeRoleFromUser(userId: Long, roleName: String, updatedBy: Long): UserDTO {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId.toString(), "User not found") }

        // 1. Find the active role mapping
        val roleMapping = userEntity.userRoles.find {
            it.role.name == roleName && !it.isDeleted
        }

        if (roleMapping != null) {
            roleMapping.markDeleted(updatedBy)
            try {
                userRepository.save(userEntity)
                logger.info("Soft deleted role $roleName from user ${userEntity.username} by admin $updatedBy")
            }catch (e: Exception){
                logger.error("unable to soft delete role $roleName from user ${userEntity.username} by admin $updatedBy", e)
                throw RuntimeException("unable to soft delete role $roleName from user ${userEntity.username} by admin $updatedBy", e)
            }
        } else {
            logger.warn("Role $roleName not found or already deleted for user ${userEntity.username}")
            throw RoleNotFoundException("Role $roleName not found or already deleted for user ${userEntity.username}")
        }

        return userEntity.toDomain().toDTO()
    }
}