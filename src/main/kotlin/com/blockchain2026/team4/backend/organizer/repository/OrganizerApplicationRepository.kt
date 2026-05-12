package com.blockchain2026.team4.backend.organizer.repository

import com.blockchain2026.team4.backend.organizer.entity.OrganizerApplicationEntity
import com.blockchain2026.team4.backend.organizer.entity.OrganizerApplicationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrganizerApplicationRepository : JpaRepository<OrganizerApplicationEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<OrganizerApplicationEntity>

    fun findAllByStatus(status: OrganizerApplicationStatus, pageable: Pageable): Page<OrganizerApplicationEntity>

    fun existsByUserIdAndStatus(userId: UUID, status: OrganizerApplicationStatus): Boolean
}
