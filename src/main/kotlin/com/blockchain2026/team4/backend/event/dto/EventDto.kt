package com.blockchain2026.team4.backend.event.dto

import com.blockchain2026.team4.backend.event.entity.EventStatus
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

data class EventDto(
    val id: UUID,
    val organizerId: UUID,
    val contractEventId: BigInteger?,
    val name: String,
    val description: String?,
    val category: String,
    val venue: String,
    val imageUrl: String?,
    val eventAt: Instant,
    val eventStartAt: Instant,
    val eventEndAt: Instant,
    val ticketPriceWei: BigInteger,
    val totalTicketCount: Int,
    val remainingTicketCount: Int,
    val soldTicketCount: Int,
    val primarySaleStart: Instant,
    val primarySaleEnd: Instant,
    val resaleAllowed: Boolean,
    val maxResalePriceRate: Int,
    val resaleStart: Instant?,
    val resaleEnd: Instant?,
    val flagged: Boolean,
    val adminCanceled: Boolean,
    val status: EventStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)
