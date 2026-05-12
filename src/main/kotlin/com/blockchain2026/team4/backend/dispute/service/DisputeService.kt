package com.blockchain2026.team4.backend.dispute.service

import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.dispute.dto.DisputeCreateCommand
import com.blockchain2026.team4.backend.dispute.dto.DisputeDto
import com.blockchain2026.team4.backend.dispute.dto.DisputeReviewCommand
import com.blockchain2026.team4.backend.dispute.entity.DisputeEntity
import com.blockchain2026.team4.backend.dispute.entity.DisputeStatus
import com.blockchain2026.team4.backend.dispute.mapper.DisputeMapper
import com.blockchain2026.team4.backend.dispute.repository.DisputeRepository
import com.blockchain2026.team4.backend.resale.service.ResaleService
import com.blockchain2026.team4.backend.ticket.service.TicketService
import com.blockchain2026.team4.backend.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class DisputeService(
    private val disputeRepository: DisputeRepository,
    private val userService: UserService,
    private val resaleService: ResaleService,
    private val ticketService: TicketService,
    private val disputeMapper: DisputeMapper,
) {
    @Transactional
    fun create(reporterId: UUID, command: DisputeCreateCommand): DisputeDto {
        if (command.resaleListingId == null && command.ticketId == null) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "리셀 등록 또는 티켓 중 하나는 반드시 필요합니다.")
        }
        val reporter = userService.findEntity(reporterId)
        val listing = command.resaleListingId?.let(resaleService::findEntity)
        val ticket = command.ticketId?.let(ticketService::findEntity) ?: listing?.ticket
        return disputeMapper.toDto(
            disputeRepository.save(
                DisputeEntity(
                    reporter = reporter,
                    resaleListing = listing,
                    ticket = ticket,
                    type = command.type,
                    description = command.description,
                ),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun listMine(reporterId: UUID, page: Int, size: Int): PageResponse<DisputeDto> {
        val disputes = disputeRepository.findAllByReporterId(reporterId, PageRequest.of(page, size))
        return disputes.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(status: DisputeStatus?, page: Int, size: Int): PageResponse<DisputeDto> {
        val pageable = PageRequest.of(page, size)
        val disputes = status?.let { disputeRepository.findAllByStatus(it, pageable) } ?: disputeRepository.findAll(pageable)
        return disputes.toResponse()
    }

    @Transactional
    fun review(adminId: UUID, disputeId: UUID, command: DisputeReviewCommand): DisputeDto {
        val dispute = disputeRepository.findById(disputeId)
            .orElseThrow { BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "분쟁 신고를 찾을 수 없습니다.") }
        dispute.status = command.status
        dispute.resolutionNote = command.resolutionNote
        dispute.reviewedBy = adminId
        dispute.reviewedAt = Instant.now()
        return disputeMapper.toDto(dispute)
    }

    private fun org.springframework.data.domain.Page<DisputeEntity>.toResponse(): PageResponse<DisputeDto> =
        PageResponse(
            items = content.map(disputeMapper::toDto),
            page = number,
            size = this.size,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = hasNext(),
        )
}
