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

    private val sharedSecret = internalProperties.sharedSecret

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val logger = LoggerFactory.getLogger(InternalServiceFilter::class.java)
        val path = request.requestURI

        if (path.startsWith("/api/internal")) {
            val serviceId = request.getHeader("X-Internal-Service-ID")
            val signature = request.getHeader("X-Service-Signature")
            val timestampStr = request.getHeader("X-Service-Timestamp")

            if (serviceId.isNullOrBlank() || signature.isNullOrBlank() || timestampStr.isNullOrBlank()) {
                logger.warn("Blocked internal request to $path: Missing required S2S headers.")
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing Internal Headers")
                return
            }

            val timestamp = try {
                timestampStr.toLong()
            } catch (_: NumberFormatException) {
                logger.warn("Blocked internal request to $path: Malformed timestamp.")
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed Timestamp")
                return
            }

            // The True HMAC Verification
            val isValidToken = S2SAuthUtil.validateToken(
                signature = signature,
                serviceId = serviceId,
                timestamp = timestamp,
                sharedSecret = sharedSecret
            )

            if (!isValidToken) {
                logger.warn("Blocked internal request to $path: Invalid signature or expired token for '$serviceId'.")
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or Expired Internal Token")
                return
            }

            logger.debug("Successfully authenticated internal request from service: $serviceId")
        }

        filterChain.doFilter(request, response)
    }
}
