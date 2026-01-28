package com.sam_the_dev.eventhive.infrastructure.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Profile("!test")
@Service
class RateLimitingService {
    // Store buckets in memory: Map<IP_Address, Bucket>
    private val cache = ConcurrentHashMap<String, Bucket>()

    fun resolveBucket(ip: String): Bucket {
        return cache.computeIfAbsent(ip) { _ -> newBucket() }
    }

    private fun newBucket(): Bucket {
        // Rule: 20 requests per minute per IP
        // "Capacity" = max tokens the bucket can hold
        // "Refill" = speed at which tokens are added back
        val limit =  Bandwidth.builder()
            .capacity(20)
            .refillGreedy(20, Duration.ofMinutes(1))
            .build()

        return Bucket.builder()
            .addLimit(limit)
            .build()
    }
}
