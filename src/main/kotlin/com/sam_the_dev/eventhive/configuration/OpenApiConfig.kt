package com.sam_the_dev.eventhive.configuration

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    companion object {
        private const val SECURITY_SCHEME_NAME = "bearerAuth"
    }

    @Bean
    fun customOpenAPI(): OpenAPI {

        return OpenAPI()
            .info(
                Info()
                    .title("EventHive API 🐝")
                    .description("API documentation for EventHive - A robust event booking platform.")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Naveen")
                            .email("naveenrameshcud@gmail.com")
                            .url("https://github.com/Naveen2070/EventHive")
                    )
            )
            // This sets up the "Authorize" button
            .addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(
                Components()
                    .addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        SecurityScheme()
                            .name(SECURITY_SCHEME_NAME)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
    }
}