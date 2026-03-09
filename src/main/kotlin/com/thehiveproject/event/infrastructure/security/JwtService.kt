package com.thehiveproject.event.infrastructure.security

import com.thehiveproject.event.configuration.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    jwtProperties: JwtProperties
) {

    private var secretKey: String = jwtProperties.secret

    private var expirationMs: Long = jwtProperties.expirationMs

    // 1. Generate Token
    fun generateToken(userDetails: UserDetails): String {
        return generateToken(emptyMap(), userDetails)
    }

    fun generateToken(
        extraClaims: Map<String, Any>,
        userDetails: UserDetails,
    ): String {
        val roles = userDetails.authorities.map { it.authority }

        val combinedClaims = extraClaims + mapOf(
            "roles" to roles
        )
        return Jwts.builder()
            .claims(combinedClaims)
            .subject(userDetails.username)
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact()
    }

    // 2. Validate Token
    fun isTokenValid(token: String): Boolean {
        return !isTokenExpired(token)
    }

    // 3. Extract Username (Email)
    fun extractUsername(token: String): String {
        return extractClaim(token, Claims::getSubject)
    }

    // Helper functions
    private fun <T> extractClaim(
        token: String,
        claimsResolver: (Claims) -> T
    ): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    fun extractUserId(token: String): Long {
        val claims = extractAllClaims(token)
        return claims["id"].toString().toLong()
    }

    fun extractRoles(token: String): List<String> {
        val claims = extractAllClaims(token)
        return when (val rawRoles = claims["roles"]) {
            is List<*> -> rawRoles.map { it.toString() }
            is String -> listOf(rawRoles)
            else -> emptyList()
        }
    }

    fun extractPermissions(token: String): Map<String, List<String>> {
        val claims = extractAllClaims(token)
        val rawPermissions = claims["permissions"] as? Map<*, *> ?: return emptyMap()

        val result = mutableMapOf<String, List<String>>()
        rawPermissions.forEach { (key, value) ->
            if (key is String) {
                val roles = when (value) {
                    is List<*> -> value.map { it.toString() }
                    is String -> listOf(value)
                    else -> emptyList()
                }
                result[key] = roles
            }
        }
        return result
    }

    fun hasAnyRole(token: String, vararg requiredRoles: String): Boolean {
        val claims = extractAllClaims(token)

        // Normalize required roles with ROLE_ prefix
        val normalizedRequired = requiredRoles.map { if (it.startsWith("ROLE_")) it else "ROLE_$it" }

        // 1. Check legacy 'roles' claim
        val legacyRoles = when (val rawRoles = claims["roles"]) {
            is List<*> -> rawRoles.map { it.toString() }
            is String -> listOf(rawRoles)
            else -> emptyList()
        }
        val normalizedLegacy = legacyRoles.map { if (it.startsWith("ROLE_")) it else "ROLE_$it" }
        if (normalizedLegacy.any { it in normalizedRequired }) return true

        // 2. Check 'permissions' map for 'events' domain
        val permissions = extractPermissions(token)
        val eventRoles = permissions["events"] ?: emptyList()
        val normalizedEventRoles = eventRoles.map { if (it.startsWith("ROLE_")) it else "ROLE_$it" }

        return normalizedEventRoles.any { it in normalizedRequired }
    }


    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }

    private fun extractExpiration(token: String): Date {
        return extractClaim(token, Claims::getExpiration)
    }

    private fun getSigningKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(secretKey)
        return Keys.hmacShaKeyFor(keyBytes)
    }
}
