package com.thehiveproject.event.infrastructure.security


import com.thehiveproject.event.configuration.InternalProperties
import com.thehiveproject.event.infrastructure.utils.S2SAuthUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class InternalServiceFilter(
    internalProperties: InternalProperties
) : OncePerRequestFilter() {
    val sharedSecret = internalProperties.sharedSecret

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val logger = LoggerFactory.getLogger(InternalServiceFilter::class.java)

        val path = request.requestURI

        // 1. Only enforce on internal paths
        if (path.startsWith("/api/internal")) {
            val serviceId = request.getHeader("X-Internal-Service-ID")
            val serviceToken = request.getHeader("X-Service-Token")

            // 2. Reject if headers are missing
            if (serviceId.isNullOrBlank() || serviceToken.isNullOrBlank()) {
                logger.warn("Blocked internal request to $path: Missing required S2S headers.")
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing Internal Headers")
                return
            }

            // 3. Verify the token signature
            val isValidToken = S2SAuthUtil.compareToken(
                token = serviceToken,
                serviceId,
                sharedSecret
            )
            if (isValidToken) {
                logger.warn("Blocked internal request to $path: Invalid S2S token for service '$serviceId'.")
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Internal Token")
                return
            }

            logger.debug("Successfully authenticated internal request from service: $serviceId")
        }

        filterChain.doFilter(request, response)
    }
}