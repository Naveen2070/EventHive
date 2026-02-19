package com.thehiveproject.event.infrastructure.utils

import java.security.MessageDigest
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/**
 * Utility object for secure Service-to-Service (S2S) authentication.
 *
 * This implementation uses HMAC-SHA256 signatures combined with a timestamp
 * to provide:
 *
 * - Strong cryptographic integrity (via HMAC)
 * - Replay attack protection (via timestamp validation)
 * - Timing attack resistance (via constant-time comparison)
 *
 * Signature Formula:
 * `Base64(HMAC-SHA256(serviceId + ":" + timestamp, sharedSecret))`
 *
 * A valid request must include:
 * - serviceId
 * - timestamp (epoch seconds)
 * - signature
 *
 * The server recalculates the signature and verifies:
 * 1. The timestamp is within the allowed time window.
 * 2. The signature matches using constant-time comparison.
 */
object S2SAuthUtil {

    private const val HMAC_ALGO = "HmacSHA256"

    /**
     * The default maximum time window (in seconds) that a token is considered valid.
     *
     * This helps prevent replay attacks by rejecting tokens that are:
     * - Too old
     * - Too far in the future
     */
    private const val ALLOWED_CLOCK_SKEW_SECONDS = 60L

    /**
     * Generates a Base64-encoded HMAC-SHA256 signature for service-to-service authentication.
     *
     * The signature is created using the following formula:
     *
     * `Base64(HMAC-SHA256(serviceId + ":" + timestamp, sharedSecret))`
     *
     * @param serviceId the unique identifier of the calling service
     * @param timestamp the request timestamp in epoch seconds
     * @param sharedSecret the shared secret key associated with the service
     * @return a Base64-encoded HMAC-SHA256 signature
     */
    fun generateSignature(serviceId: String, timestamp: Long, sharedSecret: String): String {
        val payload = "$serviceId:$timestamp"
        val secretKeySpec = SecretKeySpec(sharedSecret.toByteArray(Charsets.UTF_8), HMAC_ALGO)

        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(secretKeySpec)

        val hashBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    /**
     * Validates a service-to-service authentication token.
     *
     * Validation consists of:
     *
     * 1. **Replay protection** — Ensures the timestamp is within the allowed time window.
     * 2. **Signature verification** — Recomputes the expected HMAC signature.
     * 3. **Timing attack protection** — Uses constant-time comparison to avoid side-channel leaks.
     *
     * A token is considered valid only if:
     * - The timestamp difference from the current time does not exceed [maxAgeSeconds].
     * - The provided signature matches the expected signature.
     *
     * @param signature the Base64-encoded HMAC signature provided by the client
     * @param serviceId the unique identifier of the calling service
     * @param timestamp the request timestamp in epoch seconds
     * @param sharedSecret the shared secret key associated with the service
     * @param maxAgeSeconds the allowed clock skew in seconds (default: 60 seconds)
     * @return true if the token is valid and within the allowed time window, false otherwise
     */
    fun validateToken(
        signature: String,
        serviceId: String,
        timestamp: Long,
        sharedSecret: String,
        maxAgeSeconds: Long = ALLOWED_CLOCK_SKEW_SECONDS
    ): Boolean {
        val now = Instant.now().epochSecond

        // 1. Replay Attack Prevention (Is the token too old or from the future?)
        if (abs(now - timestamp) > maxAgeSeconds) {
            return false
        }

        // 2. Recalculate the expected signature
        val expectedSignature = generateSignature(serviceId, timestamp, sharedSecret)

        // 3. Constant-time comparison to prevent timing side-channel attacks
        return MessageDigest.isEqual(
            expectedSignature.toByteArray(Charsets.UTF_8),
            signature.toByteArray(Charsets.UTF_8)
        )
    }
}
