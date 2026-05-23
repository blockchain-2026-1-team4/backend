package com.blockchain2026.team4.backend.dispute.repository

import com.blockchain2026.team4.backend.dispute.entity.DisputeEntity
import com.blockchain2026.team4.backend.dispute.entity.DisputeStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DisputeRepository : JpaRepository<DisputeEntity, UUID> {
    fun findAllByReporterId(reporterId: UUID, pageable: Pageable): Page<DisputeEntity>

    fun findAllByStatus(status: DisputeStatus, pageable: Pageable): Page<DisputeEntity>

    fun existsByReporterIdAndResaleListingIdAndStatusIn(
        reporterId: UUID,
        resaleListingId: UUID,
        statuses: Collection<DisputeStatus>,
    ): Boolean

    fun existsByReporterIdAndTicketIdAndStatusIn(
        reporterId: UUID,
        ticketId: UUID,
        statuses: Collection<DisputeStatus>,
    ): Boolean
}
