package com.thehiveproject.event.configuration

import com.thehiveproject.event.infrastructure.utils.S2SAuthUtil
import feign.RequestInterceptor
import feign.RequestTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
            // 1. Generate the secure token
            val token = S2SAuthUtil.generateToken(serviceId, sharedSecret)

            // 2. Attach Headers
            template.header("X-Internal-Service-ID", serviceId)
            template.header("X-Service-Token", token)

            logger.debug("Attached S2S headers for service: $serviceId")
        }
    }
}