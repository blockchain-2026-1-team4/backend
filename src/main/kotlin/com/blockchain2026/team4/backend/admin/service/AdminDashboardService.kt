package com.blockchain2026.team4.backend.admin.service

import com.blockchain2026.team4.backend.admin.controller.response.AdminDashboardResponse
import com.blockchain2026.team4.backend.event.service.EventService
import com.blockchain2026.team4.backend.resale.service.ResaleService
import com.blockchain2026.team4.backend.ticket.service.TicketService
import org.springframework.stereotype.Service

@Service
class AdminDashboardService(
    private val eventService: EventService,
    private val ticketService: TicketService,
    private val resaleService: ResaleService,
) {
    fun dashboard(): AdminDashboardResponse =
        AdminDashboardResponse(
            activeEventCount = eventService.countActive(),
            soldTicketCount = ticketService.countSold(),
            usedTicketCount = ticketService.countUsed(),
            activeResaleListingCount = resaleService.countActive(),
        )
}
