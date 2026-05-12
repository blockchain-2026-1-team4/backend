package com.blockchain2026.team4.backend.event.dto

import java.time.Instant
import java.util.UUID

data class EventValidatorDto(
    val id: UUID,
    val eventId: UUID,
    val validatorId: UUID,
    val validatorWalletAddress: String?,
    val validatorEmail: String?,
    val createdAt: Instant,
)
