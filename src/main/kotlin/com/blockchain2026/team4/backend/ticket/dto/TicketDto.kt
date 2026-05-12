package com.blockchain2026.team4.backend.ticket.dto

import com.blockchain2026.team4.backend.ticket.entity.TicketStatus
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

data class TicketDto(
    val id: UUID,
    val eventId: UUID,
    val ownerId: UUID?,
    val contractTokenId: BigInteger?,
    val seatInfo: String,
    val originalPriceWei: BigInteger,
    val status: TicketStatus,
    val usedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
