package com.sam_the_dev.eventhive.configuration

import com.sam_the_dev.eventhive.infrastructure.security.RateLimitInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableRetry
@EnableAsync
@EnableSpringDataWebSupport(
    pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
class WebConfig(
    private val rateLimitInterceptor: RateLimitInterceptor
): WebMvcConfigurer{
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**") // Apply to all API endpoints
            // Optional: Exclude Swagger UI or static assets
            .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**")
    }
}
