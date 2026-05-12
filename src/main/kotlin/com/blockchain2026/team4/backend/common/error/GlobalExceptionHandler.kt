package com.blockchain2026.team4.backend.common.error

import com.blockchain2026.team4.backend.common.api.ApiErrorResponse
import com.blockchain2026.team4.backend.common.api.ApiFieldError
import com.blockchain2026.team4.backend.common.api.ApiMeta
import com.blockchain2026.team4.backend.common.api.ApiStackTraceLine
import com.blockchain2026.team4.backend.common.config.AppProperties
import com.blockchain2026.team4.backend.common.web.RequestIdFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler(
    private val appProperties: AppProperties,
) {
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        exception: BusinessException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> =
        errorResponse(exception.errorCode.status, exception.errorCode.name, exception.message, request, exception)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val errors = exception.bindingResult.fieldErrors.map {
            ApiFieldError(
                field = it.field,
                rejectedValue = it.rejectedValue?.toString(),
                reason = it.defaultMessage ?: "유효하지 않은 값입니다.",
            )
        }
        return errorResponse(
            status = HttpStatus.BAD_REQUEST,
            code = ErrorCode.INVALID_REQUEST.name,
            message = ErrorCode.INVALID_REQUEST.defaultMessage,
            request = request,
            exception = exception,
            errors = errors,
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        exception: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val errors = exception.constraintViolations.map {
            ApiFieldError(
                field = it.propertyPath.toString(),
                rejectedValue = it.invalidValue?.toString(),
                reason = it.message,
            )
        }
        return errorResponse(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST.name, ErrorCode.INVALID_REQUEST.defaultMessage, request, exception, errors)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(
        exception: BadCredentialsException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> =
        errorResponse(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.name, "인증 정보가 올바르지 않습니다.", request, exception)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        exception: AccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> =
        errorResponse(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.name, ErrorCode.FORBIDDEN.defaultMessage, request, exception)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        exception: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> =
        errorResponse(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST.name, "요청 파라미터 타입이 올바르지 않습니다.", request, exception)

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElement(
        exception: NoSuchElementException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> =
        errorResponse(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.name, exception.message ?: ErrorCode.RESOURCE_NOT_FOUND.defaultMessage, request, exception)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> =
        errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.name, ErrorCode.INTERNAL_ERROR.defaultMessage, request, exception)

    private fun errorResponse(
        status: HttpStatus,
        code: String,
        message: String,
        request: HttpServletRequest,
        exception: Throwable,
        errors: List<ApiFieldError> = emptyList(),
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(
            ApiErrorResponse(
                status = status.value(),
                code = code,
                message = message,
                path = request.requestURI,
                errors = errors,
                stackTrace = stackTrace(exception),
                meta = ApiMeta(RequestIdFilter.currentRequestId()),
            ),
        )

    private fun stackTrace(exception: Throwable): List<ApiStackTraceLine> {
        if (!appProperties.errors.includeStackTrace) return emptyList()
        return exception.stackTrace
            .asSequence()
            .filter { it.className.startsWith("com.blockchain2026.team4.backend") }
            .take(appProperties.errors.stackTraceDepth)
            .map {
                ApiStackTraceLine(
                    declaringClass = it.className,
                    methodName = it.methodName,
                    fileName = it.fileName,
                    lineNumber = it.lineNumber,
                )
            }
            .toList()
    }
}
