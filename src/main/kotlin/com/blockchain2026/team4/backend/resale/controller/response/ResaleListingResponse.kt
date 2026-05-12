package com.blockchain2026.team4.backend.resale.controller.response

import com.blockchain2026.team4.backend.resale.entity.ResaleListingStatus
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

data class ResaleListingResponse(
    val id: UUID,
    val ticketId: UUID,
    val sellerId: UUID,
    val eventId: UUID,
    val priceWei: BigInteger,
    val status: ResaleListingStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)
