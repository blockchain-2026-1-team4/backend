package com.blockchain2026.team4.backend.ticket.controller.request

import jakarta.validation.constraints.NotEmpty
import java.math.BigInteger

data class TicketIssueRequest(
    val seatInfos: List<String> = emptyList(),
    val totalTicketCount: Int? = null,
    val ticketSections: List<TicketSectionIssueRequest> = emptyList(),
)

data class TicketSectionIssueRequest(
    val sectionName: String,
    val priceWei: BigInteger,
    val resaleEnabled: Boolean = false,
    val resaleCapRate: Int = 10_000,
    val startNumber: Int = 1,
    val quantity: Int,
)

data class TicketPurchaseRequest(
    val memo: String? = null,
)
