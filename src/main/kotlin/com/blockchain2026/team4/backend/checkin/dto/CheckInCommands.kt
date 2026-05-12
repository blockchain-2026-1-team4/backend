package com.blockchain2026.team4.backend.checkin.dto

import java.time.Instant
import java.util.UUID

data class QrCreateCommand(
    val ticketId: UUID,
    val claimedOwner: String,
    val expiresAt: Instant,
    val signature: String,
)

data class CheckInCommand(
    val ticketId: UUID,
    val claimedOwner: String,
    val expiresAt: Instant,
    val signature: String,
    val memo: String?,
)

data class QrCodeDto(
    val ticketId: UUID,
    val payload: String,
    val qrPngBase64: String,
    val expiresAt: Instant,
)
