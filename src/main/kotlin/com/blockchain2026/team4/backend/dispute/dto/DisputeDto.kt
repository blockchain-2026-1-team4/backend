package com.blockchain2026.team4.backend.dispute.dto

import com.blockchain2026.team4.backend.dispute.entity.DisputeStatus
import com.blockchain2026.team4.backend.dispute.entity.DisputeType
import java.time.Instant
import java.util.UUID

data class DisputeDto(
    val id: UUID,
    val reporterId: UUID,
    val resaleListingId: UUID?,
    val ticketId: UUID?,
    val type: DisputeType,
    val description: String,
    val status: DisputeStatus,
    val resolutionNote: String?,
    val reviewedBy: UUID?,
    val reviewedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
