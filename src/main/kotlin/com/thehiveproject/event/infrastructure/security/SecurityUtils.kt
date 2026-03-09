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
}
