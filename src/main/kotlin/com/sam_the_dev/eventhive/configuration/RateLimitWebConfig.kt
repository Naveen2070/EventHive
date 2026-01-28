package com.sam_the_dev.eventhive.configuration

import com.sam_the_dev.eventhive.infrastructure.security.RateLimitInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@Profile("!test")
class RateLimitWebConfig(
    private val rateLimitInterceptor: RateLimitInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**")
    }
}
