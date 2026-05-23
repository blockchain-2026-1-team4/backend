package com.blockchain2026.team4.backend.dispute.facade

import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.dispute.controller.request.DisputeCreateRequest
import com.blockchain2026.team4.backend.dispute.controller.request.DisputeReviewRequest
import com.blockchain2026.team4.backend.dispute.controller.request.DisputeUpdateRequest
import com.blockchain2026.team4.backend.dispute.controller.response.DisputeResponse
import com.blockchain2026.team4.backend.dispute.dto.DisputeCreateCommand
import com.blockchain2026.team4.backend.dispute.dto.DisputeReviewCommand
import com.blockchain2026.team4.backend.dispute.dto.DisputeUpdateCommand
import com.blockchain2026.team4.backend.dispute.entity.DisputeStatus
import com.blockchain2026.team4.backend.dispute.mapper.DisputeApiMapper
import com.blockchain2026.team4.backend.dispute.service.DisputeService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DisputeFacade(
    private val disputeService: DisputeService,
    private val disputeApiMapper: DisputeApiMapper,
) {
    fun create(reporterId: UUID, request: DisputeCreateRequest): DisputeResponse =
        disputeApiMapper.toResponse(
            disputeService.create(
                reporterId,
                DisputeCreateCommand(request.resaleListingId, request.ticketId, request.type, request.description),
            ),
        )

    fun listMine(reporterId: UUID, page: Int, size: Int): PageResponse<DisputeResponse> =
        disputeService.listMine(reporterId, page, size).map()

    fun list(status: DisputeStatus?, page: Int, size: Int): PageResponse<DisputeResponse> =
        disputeService.list(status, page, size).map()

    fun update(reporterId: UUID, disputeId: UUID, request: DisputeUpdateRequest): DisputeResponse =
        disputeApiMapper.toResponse(disputeService.update(reporterId, disputeId, DisputeUpdateCommand(request.type, request.description)))

    fun review(adminId: UUID, disputeId: UUID, request: DisputeReviewRequest): DisputeResponse =
        disputeApiMapper.toResponse(disputeService.review(adminId, disputeId, DisputeReviewCommand(request.status, request.resolutionNote)))

    private fun PageResponse<com.blockchain2026.team4.backend.dispute.dto.DisputeDto>.map(): PageResponse<DisputeResponse> =
        PageResponse(
            items = disputeApiMapper.toResponses(items),
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = hasNext,
        )
}
