package com.thehiveproject.event.configuration

import com.thehiveproject.event.infrastructure.security.JwtAuthenticationFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // Disable CSRF for REST APIs
            .authorizeHttpRequests { auth ->
                auth
                    // ------------------------------- PUBLIC endpoints ----------------------------------
                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/redoc.html",
                        "/scalar.html"
                    ).permitAll()
                    // auth endpoint
                    .requestMatchers("/api/auth/**", "/api/user/forgot-password", "/api/user/reset-password").permitAll()
                    // payment webHook
                    .requestMatchers("/api/bookings/webhook/payment").permitAll()
                    // events public endpoints
                    .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()

                    // ------------------------------- RESTRICTED endpoints -------------------------------
                    .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                    // Explicitly requiring authentication at the security filter level for
                    // write operations on /api/events/** (POST/PUT/DELETE/PATCH).
                    // Although fine-grained authorization is handled in controllers via
                    // @PreAuthorize, these matchers act as a first security gate to ensure
                    // requests are authenticated before reaching controller logic (defense-in-depth).
                    .requestMatchers(HttpMethod.POST, "/api/events/**").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/events/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/events/**").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/events/**").authenticated()

                    // Secure everything else
                    .anyRequest().authenticated()
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { _, response, authException ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.contentType = "application/json"
                    response.writer.write("""{"error":"Unauthorized","message":"${authException.message}"}""")
                }
            }
        return http.build()
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }
}