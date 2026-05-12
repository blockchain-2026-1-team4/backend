package com.blockchain2026.team4.backend.common.security

import com.blockchain2026.team4.backend.common.error.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class ApiAuthenticationEntryPoint(
    private val securityErrorResponseWriter: SecurityErrorResponseWriter,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        securityErrorResponseWriter.write(
            request = request,
            response = response,
            status = HttpStatus.UNAUTHORIZED,
            errorCode = ErrorCode.UNAUTHORIZED,
            exception = authException,
        )
    }
}
