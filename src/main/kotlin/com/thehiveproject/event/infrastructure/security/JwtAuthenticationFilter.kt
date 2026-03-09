package com.thehiveproject.event.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {

    private val objectMapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)

        try {
            if (jwtService.isTokenValid(token)) {
                val username = jwtService.extractUsername(token)
                val permissions = jwtService.extractPermissions(token)
                val legacyRoles = jwtService.extractRoles(token)

                if (!permissions.containsKey("events")) {
                    log.warn("JWT rejected: 'events' domain missing for user $username")
                    sendErrorResponse(
                        response,
                        "Access Denied: Token is not valid for the events service.",
                        HttpServletResponse.SC_FORBIDDEN
                    )
                    return
                }

                // 1. Map legacy roles (e.g. ROLE_USER)
                val legacyAuthorities = legacyRoles.map { role ->
                    val finalRole = if (role.startsWith("ROLE_")) role else "ROLE_$role"
                    SimpleGrantedAuthority(finalRole)
                }

                // 2. Map domain authorities (e.g. events:ROLE_USER)
                val eventRoles = permissions["events"] ?: emptyList()
                val domainAuthorities = eventRoles.map { role ->
                    val finalRole = if (role.startsWith("ROLE_")) role else "ROLE_$role"
                    SimpleGrantedAuthority("events:$finalRole")
                }

                val authorities = legacyAuthorities + domainAuthorities

                val userId = jwtService.extractUserId(token)
                val authToken = UsernamePasswordAuthenticationToken(
                    username,
                    userId,
                    authorities
                )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authToken
            }

        } catch (ex: ExpiredJwtException) {
            log.warn("JWT token expired: ${ex.message}")
            sendErrorResponse(response, "Token has expired")
            return
        } catch (ex: Exception) {
            log.warn("JWT authentication failed: ${ex.message}")
            sendErrorResponse(response, "Invalid token")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun sendErrorResponse(
        response: HttpServletResponse,
        message: String,
        status: Int = HttpServletResponse.SC_UNAUTHORIZED
    ) {
        response.status = status
        response.contentType = "application/json"
        val errorTitle = if (status == HttpServletResponse.SC_FORBIDDEN) "Forbidden" else "Unauthorized"
        val errorResponse = mapOf("error" to errorTitle, "message" to message)
        val writer = response.writer
        writer.write(objectMapper.writeValueAsString(errorResponse))
        writer.flush()
    }
}
