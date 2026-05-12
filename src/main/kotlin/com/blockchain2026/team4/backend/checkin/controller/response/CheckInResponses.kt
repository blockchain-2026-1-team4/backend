package com.blockchain2026.team4.backend.checkin.controller.response

import com.blockchain2026.team4.backend.checkin.entity.CheckInResult
import java.time.Instant
import java.util.UUID

data class QrCodeResponse(
    val ticketId: UUID,
    val contractTokenId: String,
    val payload: String,
    val qrPngBase64: String,
    val barcodeText: String,
    val expiresAt: Instant,
)

data class CheckInMessageResponse(
    val ticketId: UUID,
    val contractTokenId: String,
    val claimedOwner: String,
    val expiresAt: Instant,
    val messageHash: String,
)

data class CheckInRecordResponse(
    val id: UUID,
    val ticketId: UUID,
    val validatorId: UUID,
    val result: CheckInResult,
    val checkedInAt: Instant,
    val memo: String?,
)
