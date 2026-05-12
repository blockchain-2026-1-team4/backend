package com.blockchain2026.team4.backend.dispute.dto

import com.blockchain2026.team4.backend.dispute.entity.DisputeStatus
import com.blockchain2026.team4.backend.dispute.entity.DisputeType
import java.util.UUID

data class DisputeCreateCommand(
    val resaleListingId: UUID?,
    val ticketId: UUID?,
    val type: DisputeType,
    val description: String,
)

data class DisputeReviewCommand(
    val status: DisputeStatus,
    val resolutionNote: String?,
)
