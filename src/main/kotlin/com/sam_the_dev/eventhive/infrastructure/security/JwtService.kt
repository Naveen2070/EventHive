package com.sam_the_dev.eventhive.infrastructure.security

import com.sam_the_dev.eventhive.configuration.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService (
    private val jwtProperties: JwtProperties
){

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
    fun isTokenValid(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return username == userDetails.username && !isTokenExpired(token)
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
