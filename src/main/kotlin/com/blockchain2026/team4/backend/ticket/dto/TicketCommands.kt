package com.blockchain2026.team4.backend.ticket.dto

import java.math.BigInteger

data class TicketSectionIssueCommand(
    val sectionName: String,
    val priceWei: BigInteger,
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
