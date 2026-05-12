package com.blockchain2026.team4.backend.organizer.dto

import com.blockchain2026.team4.backend.organizer.entity.OrganizerApplicationStatus
import java.time.Instant
import java.util.UUID

data class OrganizerApplicationDto(
    val id: UUID,
    val userId: UUID,
    val businessName: String,
    val contactEmail: String,
    val description: String?,
    val status: OrganizerApplicationStatus,
    val reviewedBy: UUID?,
    val reviewedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
