package com.blockchain2026.team4.backend.event.controller.response

import java.time.Instant
import java.util.UUID

data class EventValidatorResponse(
    val id: UUID,
    val eventId: UUID,
    val validatorId: UUID,
    val validatorWalletAddress: String?,
    val validatorEmail: String?,
    val createdAt: Instant,
)
