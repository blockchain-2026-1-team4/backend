package com.blockchain2026.team4.backend.dispute.controller.request

import com.blockchain2026.team4.backend.dispute.entity.DisputeStatus
import com.blockchain2026.team4.backend.dispute.entity.DisputeType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class DisputeCreateRequest(
    val resaleListingId: UUID?,
    val ticketId: UUID?,
    val type: DisputeType,

    @field:NotBlank
    @field:Size(max = 2000)
    val description: String,
)

data class DisputeReviewRequest(
    val status: DisputeStatus,

    @field:Size(max = 2000)
    val resolutionNote: String?,
)
