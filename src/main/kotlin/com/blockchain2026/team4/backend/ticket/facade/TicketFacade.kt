package com.blockchain2026.team4.backend.ticket.facade

import com.blockchain2026.team4.backend.ticket.controller.request.TicketCancelIssuedRequest
import com.blockchain2026.team4.backend.ticket.controller.request.TicketIssueRequest
import com.blockchain2026.team4.backend.ticket.controller.response.TicketResponse
import com.blockchain2026.team4.backend.ticket.controller.response.TicketValidityResponse
import com.blockchain2026.team4.backend.ticket.dto.TicketIssueCommand
import com.blockchain2026.team4.backend.ticket.dto.TicketSectionIssueCommand
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
        ticketApiMapper.toResponses(
            ticketService.issueTickets(
                organizerId,
                eventId,
                TicketIssueCommand(
                    seatInfos = request.seatInfos,
                    totalTicketCount = request.totalTicketCount,
                    ticketSections = request.ticketSections.map {
                        TicketSectionIssueCommand(
                            eventRoundId = it.eventRoundId,
                            sectionName = it.sectionName,
                            priceWei = it.priceWei,
                            saleStartAt = it.saleStartAt,
                            saleEndAt = it.saleEndAt,
                            resaleEnabled = it.resaleEnabled,
                            resaleCapRate = it.resaleCapRate,
                            startNumber = it.startNumber,
                            quantity = it.quantity,
                        )
                    },
                ),
            ),
        )

    fun cancelIssuedTickets(organizerId: UUID, eventId: UUID, request: TicketCancelIssuedRequest): List<TicketResponse> =
        ticketApiMapper.toResponses(ticketService.cancelIssuedTickets(organizerId, eventId, request.ticketIds))

    fun purchase(userId: UUID, ticketId: UUID): TicketResponse =
        ticketApiMapper.toResponse(ticketService.purchaseTicket(userId, ticketId))

    fun get(ticketId: UUID): TicketResponse = ticketApiMapper.toResponse(ticketService.get(ticketId))

    fun listByEvent(eventId: UUID): List<TicketResponse> = ticketApiMapper.toResponses(ticketService.listByEvent(eventId))

    fun listMine(userId: UUID): List<TicketResponse> = ticketApiMapper.toResponses(ticketService.listMine(userId))

    fun listByOwnerWallet(walletAddress: String): List<TicketResponse> =
        ticketApiMapper.toResponses(ticketService.listByOwnerWallet(walletAddress))

    fun validity(ticketId: UUID): TicketValidityResponse = ticketApiMapper.toResponse(ticketService.validity(ticketId))
}
