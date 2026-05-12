package com.blockchain2026.team4.backend.organizer.dto

import com.blockchain2026.team4.backend.organizer.entity.OrganizerApplicationStatus

data class OrganizerApplicationCommand(
    val businessName: String,
    val contactEmail: String,
    val description: String?,
)

data class OrganizerReviewCommand(
    val status: OrganizerApplicationStatus,
)
