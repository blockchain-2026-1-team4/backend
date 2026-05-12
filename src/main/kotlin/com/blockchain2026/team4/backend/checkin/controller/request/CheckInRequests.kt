package com.blockchain2026.team4.backend.checkin.controller.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class QrCreateRequest(
    @field:NotBlank
    val claimedOwner: String,

    @field:NotNull
    val expiresAt: Instant,

    @field:NotBlank
    val signature: String,
)

data class CheckInRequest(
    @field:NotNull
    val ticketId: UUID,

    @field:NotBlank
    val claimedOwner: String,

    @field:NotNull
    val expiresAt: Instant,

    @field:NotBlank
    val signature: String,

    val memo: String?,
)
