package com.thehiveproject.event.configuration

import com.thehiveproject.event.infrastructure.utils.S2SAuthUtil
import feign.RequestInterceptor
import feign.RequestTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant

@Configuration
class FeignConfig(
    internalProperties: InternalProperties,
    @param:Value("\${spring.application.name}") private val serviceId: String
) {
    val sharedSecret = internalProperties.sharedSecret
    private val logger = LoggerFactory.getLogger(FeignConfig::class.java)

    @Bean
    fun requestInterceptor(): RequestInterceptor {
        return RequestInterceptor { template: RequestTemplate ->

            // 1. Capture the exact second this request is being made
            val currentTimestamp = Instant.now().epochSecond

            // 2. Generate the HMAC signature
            val signature = S2SAuthUtil.generateSignature(serviceId, currentTimestamp, sharedSecret)

            // 3. Attach all three required headers
            template.header("X-Internal-Service-ID", serviceId)
            template.header("X-Service-Timestamp", currentTimestamp.toString())
            template.header("X-Service-Signature", signature)

            logger.debug("Attached S2S HMAC headers for service: $serviceId")
        }
    }
}