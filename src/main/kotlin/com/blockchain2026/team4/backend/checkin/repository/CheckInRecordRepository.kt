package com.blockchain2026.team4.backend.checkin.repository

import com.blockchain2026.team4.backend.checkin.entity.CheckInRecordEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CheckInRecordRepository : JpaRepository<CheckInRecordEntity, UUID> {
    fun findAllByTicketId(ticketId: UUID): List<CheckInRecordEntity>
}
