package com.blockchain2026.team4.backend.checkin.controller.response

import com.blockchain2026.team4.backend.checkin.entity.CheckInResult
import java.time.Instant
import java.util.UUID

data class QrCodeResponse(
    val ticketId: UUID,
    val payload: String,
    val qrPngBase64: String,
    val expiresAt: Instant,
)

data class CheckInRecordResponse(
    val id: UUID,
    val ticketId: UUID,
    val validatorId: UUID,
    val result: CheckInResult,
    val checkedInAt: Instant,
    val memo: String?,
)
