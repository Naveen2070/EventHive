package com.thehiveproject.event.infrastructure.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
object SecurityUtils {

    /**
     * Retrieves the current authenticated user's ID from the SecurityContext.
     * The ID is stored in the 'credentials' field of the UsernamePasswordAuthenticationToken
     * by the JwtAuthenticationFilter.
     *
     * @return The Long userId of the current user.
     * @throws IllegalStateException if the user is not authenticated.
     */
    fun getCurrentUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated) {
            throw IllegalStateException("No authenticated user found in SecurityContext")
        }

        return authentication.credentials as? Long
            ?: throw IllegalStateException("User ID not found in authentication credentials")
    }

    /**
     * Checks if the current authenticated user has any of the specified authorities.
     *
     * @param authorities Variable number of authority strings to check for.
     * @return true if the user has at least one of the specified authorities, false otherwise.
     */
    fun hasAnyAuthority(vararg authorities: String): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication ?: return false
        val userAuthorities = authentication.authorities.map { it.authority }
        return authorities.any { it in userAuthorities }
    }
}
