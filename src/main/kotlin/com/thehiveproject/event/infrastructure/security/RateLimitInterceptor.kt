package com.thehiveproject.event.infrastructure.security

import com.thehiveproject.event.api.error.RateLimitExceededException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Profile("!test")
@Component
class RateLimitInterceptor(
    private val rateLimitingService: RateLimitingService
): HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        // 1. Identify the client (User IP)
        // If behind a proxy (like Nginx/AWS LB), use "X-Forwarded-For" header
        val ip = request.getHeader("X-Forwarded-For") ?: request.remoteAddr

        // 2. Get their bucket
        val tokenBucket = rateLimitingService.resolveBucket(ip)

        // 3. Try to consume 1 token
        val probe = tokenBucket.tryConsumeAndReturnRemaining(1)

        if (probe.isConsumed) {
            // Success! Add header telling them how many tokens are left
            response.addHeader("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
            return true
        } else {
            // Failure! Bucket is empty.
            throw RateLimitExceededException("Too many requests. Please try again later.")
        }
    }
}
