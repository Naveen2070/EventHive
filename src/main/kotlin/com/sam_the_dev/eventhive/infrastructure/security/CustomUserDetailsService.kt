package com.sam_the_dev.eventhive.infrastructure.security

import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(usernameOrEmail: String): UserDetails {
        // We reuse your existing logic to find by either email or username
        val userEntity = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            ?: throw UsernameNotFoundException("User not found with username or email: $usernameOrEmail")

        // Map Roles (if you have them) to Authorities
        // Note: Assuming your UserEntity has a 'roles' relationship.
        // If not, we pass an empty list for now or a default "ROLE_USER"
        val authorities = userEntity.userRoles
            .filter { !it.isDeleted }
            .map { userRole ->
                SimpleGrantedAuthority("ROLE_${userRole.role.name}")
            }.toSet()

        // Return the Spring Security User object
        return User(
            userEntity.email,
            userEntity.password,
            userEntity.isActive,
            userEntity.isActive || !userEntity.isDeleted,
            true,
            !userEntity.isDeleted,
            authorities
        )
    }
}