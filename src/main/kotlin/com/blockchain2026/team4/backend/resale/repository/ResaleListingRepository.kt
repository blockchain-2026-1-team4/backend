package com.blockchain2026.team4.backend.resale.repository

import com.blockchain2026.team4.backend.resale.entity.ResaleListingEntity
import com.blockchain2026.team4.backend.resale.entity.ResaleListingStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ResaleListingRepository : JpaRepository<ResaleListingEntity, UUID> {
    fun findAllByStatus(status: ResaleListingStatus, pageable: Pageable): Page<ResaleListingEntity>

    fun findByTicketIdAndStatus(ticketId: UUID, status: ResaleListingStatus): ResaleListingEntity?

    fun findAllByStatusNot(status: ResaleListingStatus, pageable: Pageable): Page<ResaleListingEntity>

    fun findAllBySellerId(sellerId: UUID, pageable: Pageable): Page<ResaleListingEntity>

    fun countByStatus(status: ResaleListingStatus): Long
}
