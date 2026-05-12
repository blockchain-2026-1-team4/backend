package com.blockchain2026.team4.backend.organizer.controller.request

import com.blockchain2026.team4.backend.organizer.entity.OrganizerApplicationStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class OrganizerApplicationRequest(
    @field:NotBlank
    @field:Size(max = 180)
    val businessName: String,

    @field:Email
    @field:NotBlank
    val contactEmail: String,

    val description: String?,
)

data class OrganizerReviewRequest(
    val status: OrganizerApplicationStatus,
)
