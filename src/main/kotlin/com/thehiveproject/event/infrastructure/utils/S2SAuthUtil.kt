package com.thehiveproject.event.infrastructure.utils

import java.security.MessageDigest
import java.util.Base64

object S2SAuthUtil {
    /**
     * Generates a secure token for service-to-service authentication.
     *
     * The token is created by hashing the combination of the service ID and
     * the shared secret using SHA-256, then encoding the result in Base64.
     *
     * Formula: `Base64(SHA-256(serviceId + ":" + sharedSecret))`
     *
     * @param serviceId the unique identifier of the service
     * @param sharedSecret the shared secret key associated with the service
     * @return a Base64-encoded SHA-256 hash token
     */
    fun generateToken(serviceId: String, sharedSecret: String): String {
        val payload = "$serviceId:$sharedSecret"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(payload.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    /**
     * Compares a given token with a token generated from the service ID and shared secret.
     * Uses a constant-time comparison to prevent timing attacks.
     *
     * @param token the token to verify
     * @param serviceId the unique identifier of the service
     * @param sharedSecret the shared secret key associated with the service
     * @return true if the token matches, false otherwise
     */
    fun compareToken(token: String, serviceId: String, sharedSecret: String): Boolean {
        val generatedToken = generateToken(serviceId, sharedSecret)
        return constantTimeEquals(generatedToken, token)
    }

    /**
     * Performs a constant-time comparison of two strings to prevent timing attacks.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
