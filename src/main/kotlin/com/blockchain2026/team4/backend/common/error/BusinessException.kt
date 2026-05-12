package com.blockchain2026.team4.backend.common.error

class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.defaultMessage,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
