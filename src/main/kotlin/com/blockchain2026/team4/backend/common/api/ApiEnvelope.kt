package com.blockchain2026.team4.backend.common.api

import java.time.Instant

data class ApiSuccessResponse<T>(
    val success: Boolean = true,
    val status: Int,
    val code: String,
    val message: String,
    val data: T?,
    val meta: ApiMeta,
)

data class ApiErrorResponse(
    val success: Boolean = false,
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
    val errors: List<ApiFieldError> = emptyList(),
    val stackTrace: List<ApiStackTraceLine> = emptyList(),
    val meta: ApiMeta,
)

data class ApiMeta(
    val requestId: String,
    val timestamp: Instant = Instant.now(),
)

data class ApiFieldError(
    val field: String,
    val rejectedValue: String?,
    val reason: String,
)

data class ApiStackTraceLine(
    val declaringClass: String,
    val methodName: String,
    val fileName: String?,
    val lineNumber: Int,
)

data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)
