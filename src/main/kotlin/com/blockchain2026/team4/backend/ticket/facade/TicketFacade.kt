package com.blockchain2026.team4.backend.ticket.facade

import com.blockchain2026.team4.backend.ticket.controller.request.TicketIssueRequest
import com.blockchain2026.team4.backend.ticket.controller.response.TicketResponse
import com.blockchain2026.team4.backend.ticket.dto.TicketIssueCommand
import com.blockchain2026.team4.backend.ticket.mapper.TicketApiMapper
import com.blockchain2026.team4.backend.ticket.service.TicketService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TicketFacade(
    private val ticketService: TicketService,
    private val ticketApiMapper: TicketApiMapper,
) {
    fun issueTickets(organizerId: UUID, eventId: UUID, request: TicketIssueRequest): List<TicketResponse> =
        ticketApiMapper.toResponses(ticketService.issueTickets(organizerId, eventId, TicketIssueCommand(request.seatInfos)))

    fun purchase(userId: UUID, ticketId: UUID): TicketResponse =
        ticketApiMapper.toResponse(ticketService.purchaseTicket(userId, ticketId))

    fun get(ticketId: UUID): TicketResponse = ticketApiMapper.toResponse(ticketService.get(ticketId))

    fun listByEvent(eventId: UUID): List<TicketResponse> = ticketApiMapper.toResponses(ticketService.listByEvent(eventId))

    fun listMine(userId: UUID): List<TicketResponse> = ticketApiMapper.toResponses(ticketService.listMine(userId))
}
