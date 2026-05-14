package com.blockchain2026.team4.backend.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val devAuthenticationService: DevAuthenticationService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()

        if (!token.isNullOrBlank() && SecurityContextHolder.getContext().authentication == null) {
            runCatching {
                val principal = devAuthenticationService.authenticate(token) ?: jwtProvider.parse(token)
                val authorities = principal.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(principal, token, authorities)
            }
        }

        filterChain.doFilter(request, response)
    }
}
