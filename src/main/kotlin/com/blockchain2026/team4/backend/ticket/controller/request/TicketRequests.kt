package com.blockchain2026.team4.backend.ticket.controller.request

import jakarta.validation.constraints.NotEmpty

data class TicketIssueRequest(
    @field:NotEmpty
    val seatInfos: List<String>,
)

data class TicketPurchaseRequest(
    val memo: String? = null,
)
