package com.blockchain2026.team4.backend.common.security

import com.blockchain2026.team4.backend.common.config.AppProperties
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.common.web.RequestIdFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SecurityErrorResponseWriter(
    private val appProperties: AppProperties,
) {
    fun write(
        request: HttpServletRequest,
        response: HttpServletResponse,
        status: HttpStatus,
        errorCode: ErrorCode,
        message: String = errorCode.defaultMessage,
        exception: Throwable,
    ) {
        response.status = status.value()
        response.characterEncoding = Charsets.UTF_8.name()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(errorJson(request, status, errorCode, message, exception))
    }

    private fun errorJson(
        request: HttpServletRequest,
        status: HttpStatus,
        errorCode: ErrorCode,
        message: String,
        exception: Throwable,
    ): String =
        """
        {
          "success": false,
          "status": ${status.value()},
          "code": "${errorCode.name}",
          "message": ${message.jsonString()},
          "path": ${request.requestURI.jsonString()},
          "errors": [],
          "stackTrace": [${stackTraceJson(exception)}],
          "meta": {
            "requestId": ${RequestIdFilter.currentRequestId().jsonString()},
            "timestamp": ${Instant.now().toString().jsonString()}
          }
        }
        """.trimIndent()

    private fun stackTraceJson(exception: Throwable): String {
        if (!appProperties.errors.includeStackTrace) return ""
        return exception.stackTrace
            .asSequence()
            .filter { it.className.startsWith("com.blockchain2026.team4.backend") }
            .take(appProperties.errors.stackTraceDepth)
            .joinToString(",") {
                """
                {
                  "declaringClass": ${it.className.jsonString()},
                  "methodName": ${it.methodName.jsonString()},
                  "fileName": ${it.fileName?.jsonString() ?: "null"},
                  "lineNumber": ${it.lineNumber}
                }
                """.trimIndent()
            }
    }

    private fun String.jsonString(): String =
        buildString {
            append('"')
            this@jsonString.forEach {
                when (it) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(it)
                }
            }
            append('"')
        }
}
