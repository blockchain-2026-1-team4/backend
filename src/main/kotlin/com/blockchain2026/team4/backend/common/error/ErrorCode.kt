package com.blockchain2026.team4.backend.common.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val defaultMessage: String,
) {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "현재 상태에서는 요청을 처리할 수 없습니다."),
    WALLET_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "지갑 서명이 유효하지 않습니다."),
    ORGANIZER_APPROVAL_REQUIRED(HttpStatus.FORBIDDEN, "주최자 승인 후 사용할 수 있습니다."),
    BLOCKCHAIN_TRANSACTION_FAILED(HttpStatus.BAD_GATEWAY, "블록체인 트랜잭션 처리에 실패했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
}
