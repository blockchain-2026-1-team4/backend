package com.blockchain2026.team4.backend.common.config

import com.blockchain2026.team4.backend.common.security.ApiAccessDeniedHandler
import com.blockchain2026.team4.backend.common.security.ApiAuthenticationEntryPoint
import com.blockchain2026.team4.backend.common.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val apiAuthenticationEntryPoint: ApiAuthenticationEntryPoint,
    private val apiAccessDeniedHandler: ApiAccessDeniedHandler,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf(AbstractHttpConfigurer<*, *>::disable)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(apiAuthenticationEntryPoint)
                it.accessDeniedHandler(apiAccessDeniedHandler)
            }
            .authorizeHttpRequests {
                it.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/images/**").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/events/**", "/api/v1/resale-listings/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/tickets/me").authenticated()
                it.requestMatchers(HttpMethod.GET, "/api/v1/tickets/*", "/api/v1/tickets/*/validity", "/api/v1/tickets/*/check-in-message", "/api/v1/wallets/*/tickets").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
