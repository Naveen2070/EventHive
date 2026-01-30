package com.sam_the_dev.eventhive.configuration

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
     @field:NotBlank(message = "JWT Secret must be defined")
     val secret: String,

     @field:Positive(message = "JWT Expiration must be positive")
     val expirationMs: Long
)