package com.blockchain2026.team4.backend.resale.dto

import com.blockchain2026.team4.backend.resale.entity.ResaleListingStatus
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

data class ResaleListingDto(
    val id: UUID,
    val ticketId: UUID,
    val sellerId: UUID,
    val buyerId: UUID?,
    val eventId: UUID,
    val priceWei: BigInteger,
    val status: ResaleListingStatus,
    val purchasedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
