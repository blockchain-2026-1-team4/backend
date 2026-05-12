package com.blockchain2026.team4.backend.ticket.repository

import com.blockchain2026.team4.backend.ticket.entity.TicketEntity
import com.blockchain2026.team4.backend.ticket.entity.TicketStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TicketRepository : JpaRepository<TicketEntity, UUID> {
    fun findAllByEventId(eventId: UUID): List<TicketEntity>

    fun findAllByOwnerId(ownerId: UUID): List<TicketEntity>

    fun findAllByOwnerWalletAddressIgnoreCase(walletAddress: String): List<TicketEntity>

    fun countByEventId(eventId: UUID): Long

    fun countByStatus(status: TicketStatus): Long
}
