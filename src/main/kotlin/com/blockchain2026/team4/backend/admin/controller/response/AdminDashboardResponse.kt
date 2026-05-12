package com.blockchain2026.team4.backend.admin.controller.response

data class AdminDashboardResponse(
    val activeEventCount: Long,
    val soldTicketCount: Long,
    val usedTicketCount: Long,
    val activeResaleListingCount: Long,
)
