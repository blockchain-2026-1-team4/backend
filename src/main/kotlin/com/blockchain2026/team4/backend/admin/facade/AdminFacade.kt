package com.blockchain2026.team4.backend.admin.facade

import com.blockchain2026.team4.backend.admin.controller.response.AdminDashboardResponse
import com.blockchain2026.team4.backend.admin.controller.response.BlockchainTransactionResponse
import com.blockchain2026.team4.backend.admin.controller.response.ResaleTransactionResponse
import com.blockchain2026.team4.backend.admin.service.AdminDashboardService
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.dispute.controller.request.DisputeReviewRequest
import com.blockchain2026.team4.backend.dispute.controller.response.DisputeResponse
import com.blockchain2026.team4.backend.dispute.dto.DisputeReviewCommand
import com.blockchain2026.team4.backend.dispute.entity.DisputeStatus
import com.blockchain2026.team4.backend.dispute.mapper.DisputeApiMapper
import com.blockchain2026.team4.backend.dispute.service.DisputeService
import com.blockchain2026.team4.backend.event.controller.response.EventResponse
import com.blockchain2026.team4.backend.event.entity.EventStatus
import com.blockchain2026.team4.backend.event.mapper.EventApiMapper
import com.blockchain2026.team4.backend.event.service.EventService
import com.blockchain2026.team4.backend.resale.entity.ResaleListingStatus
import com.blockchain2026.team4.backend.resale.service.ResaleService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AdminFacade(
    private val adminDashboardService: AdminDashboardService,
    private val blockchainTransactionService: BlockchainTransactionService,
    private val eventService: EventService,
    private val eventApiMapper: EventApiMapper,
    private val resaleService: ResaleService,
    private val disputeService: DisputeService,
    private val disputeApiMapper: DisputeApiMapper,
    private val adminResponseMapper: AdminResponseMapper,
) {
    fun dashboard(): AdminDashboardResponse = adminDashboardService.dashboard()

    fun latestBlockchainTransactions(size: Int): List<BlockchainTransactionResponse> =
        adminResponseMapper.toBlockchainTransactionResponses(blockchainTransactionService.latest(size))

    fun listEvents(
        page: Int,
        size: Int,
        status: EventStatus?,
        category: String?,
        query: String?,
        flagged: Boolean?,
    ): PageResponse<EventResponse> {
        val events = eventService.list(page, size, status, category, query, flagged)
        return PageResponse(
            items = eventApiMapper.toResponses(events.items),
            page = events.page,
            size = events.size,
            totalElements = events.totalElements,
            totalPages = events.totalPages,
            hasNext = events.hasNext,
        )
    }

    fun flagEvent(eventId: UUID): EventResponse = eventApiMapper.toResponse(eventService.flag(eventId, true))

    fun unflagEvent(eventId: UUID): EventResponse = eventApiMapper.toResponse(eventService.flag(eventId, false))

    fun resaleTransactions(page: Int, size: Int, status: ResaleListingStatus?): PageResponse<ResaleTransactionResponse> {
        val transactions = resaleService.listTransactions(page, size, status)
        return PageResponse(
            items = adminResponseMapper.toResaleTransactionResponses(transactions.items),
            page = transactions.page,
            size = transactions.size,
            totalElements = transactions.totalElements,
            totalPages = transactions.totalPages,
            hasNext = transactions.hasNext,
        )
    }

    fun disputes(status: DisputeStatus?, page: Int, size: Int): PageResponse<DisputeResponse> {
        val disputes = disputeService.list(status, page, size)
        return PageResponse(
            items = disputeApiMapper.toResponses(disputes.items),
            page = disputes.page,
            size = disputes.size,
            totalElements = disputes.totalElements,
            totalPages = disputes.totalPages,
            hasNext = disputes.hasNext,
        )
    }

    fun reviewDispute(adminId: UUID, disputeId: UUID, request: DisputeReviewRequest): DisputeResponse =
        disputeApiMapper.toResponse(
            disputeService.review(
                adminId,
                disputeId,
                DisputeReviewCommand(request.status, request.resolutionNote),
            ),
        )
}
