package com.blockchain2026.team4.backend.organizer.service

import com.blockchain2026.team4.backend.blockchain.gateway.TrustTicketGateway
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.organizer.dto.OrganizerApplicationCommand
import com.blockchain2026.team4.backend.organizer.dto.OrganizerApplicationDto
import com.blockchain2026.team4.backend.organizer.dto.OrganizerReviewCommand
import com.blockchain2026.team4.backend.organizer.entity.OrganizerApplicationEntity
import com.blockchain2026.team4.backend.organizer.entity.OrganizerApplicationStatus
import com.blockchain2026.team4.backend.organizer.mapper.OrganizerApplicationMapper
import com.blockchain2026.team4.backend.organizer.repository.OrganizerApplicationRepository
import com.blockchain2026.team4.backend.user.entity.UserRole
import com.blockchain2026.team4.backend.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class OrganizerApplicationService(
    private val organizerApplicationRepository: OrganizerApplicationRepository,
    private val userService: UserService,
    private val trustTicketGateway: TrustTicketGateway,
    private val blockchainTransactionService: BlockchainTransactionService,
    private val organizerApplicationMapper: OrganizerApplicationMapper,
) {
    @Transactional
    fun apply(userId: UUID, command: OrganizerApplicationCommand): OrganizerApplicationDto {
        if (organizerApplicationRepository.existsByUserIdAndStatus(userId, OrganizerApplicationStatus.PENDING)) {
            throw BusinessException(ErrorCode.CONFLICT, "이미 심사 대기 중인 주최자 신청이 있습니다.")
        }
        val user = userService.findEntity(userId)
        return organizerApplicationMapper.toDto(
            organizerApplicationRepository.save(
                OrganizerApplicationEntity(
                    user = user,
                    businessName = command.businessName,
                    contactEmail = command.contactEmail,
                    description = command.description,
                ),
            ),
        )
    }

    @Transactional
    fun review(adminId: UUID, applicationId: UUID, command: OrganizerReviewCommand): OrganizerApplicationDto {
        val application = organizerApplicationRepository.findById(applicationId)
            .orElseThrow { BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "주최자 신청을 찾을 수 없습니다.") }
        if (application.status != OrganizerApplicationStatus.PENDING) {
            throw BusinessException(ErrorCode.CONFLICT, "이미 처리된 신청입니다.")
        }
        application.status = command.status
        application.reviewedBy = adminId
        application.reviewedAt = Instant.now()

        if (command.status == OrganizerApplicationStatus.APPROVED) {
            val user = userService.grantRole(application.user.id, UserRole.ORGANIZER)
            user.walletAddress?.let {
                val submission = trustTicketGateway.addOrganizer(it)
                blockchainTransactionService.record(submission)
            }
        }
        return organizerApplicationMapper.toDto(application)
    }

    @Transactional(readOnly = true)
    fun listMine(userId: UUID): List<OrganizerApplicationDto> =
        organizerApplicationMapper.toDtos(organizerApplicationRepository.findAllByUserId(userId))

    @Transactional(readOnly = true)
    fun list(status: OrganizerApplicationStatus?, page: Int, size: Int): PageResponse<OrganizerApplicationDto> {
        val pageable = PageRequest.of(page, size)
        val applications = status?.let { organizerApplicationRepository.findAllByStatus(it, pageable) }
            ?: organizerApplicationRepository.findAll(pageable)
        return PageResponse(
            items = applications.content.map(organizerApplicationMapper::toDto),
            page = applications.number,
            size = applications.size,
            totalElements = applications.totalElements,
            totalPages = applications.totalPages,
            hasNext = applications.hasNext(),
        )
    }
}
