package com.blockchain2026.team4.backend.ticket.dto

import java.math.BigInteger
import java.time.Instant
import java.util.UUID

data class TicketSectionIssueCommand(
    val eventRoundId: UUID?,
    val sectionName: String,
    val priceWei: BigInteger,
    val saleStartAt: Instant?,
    val saleEndAt: Instant?,
    val resaleEnabled: Boolean,
    val resaleCapRate: Int,
    val startNumber: Int,
    val quantity: Int,
)

data class TicketIssueCommand(
    val seatInfos: List<String>,
    val totalTicketCount: Int?,
    val ticketSections: List<TicketSectionIssueCommand>,
)
