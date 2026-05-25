package com.blockchain2026.team4.backend.checkin.service

import com.blockchain2026.team4.backend.blockchain.gateway.TrustTicketGateway
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.checkin.dto.CheckInCommand
import com.blockchain2026.team4.backend.checkin.dto.CheckInMessageDto
import com.blockchain2026.team4.backend.checkin.dto.CheckInRecordDto
import com.blockchain2026.team4.backend.checkin.dto.QrCodeDto
import com.blockchain2026.team4.backend.checkin.dto.QrCreateCommand
import com.blockchain2026.team4.backend.checkin.entity.CheckInRecordEntity
import com.blockchain2026.team4.backend.checkin.entity.CheckInResult
import com.blockchain2026.team4.backend.checkin.mapper.CheckInRecordMapper
import com.blockchain2026.team4.backend.checkin.repository.CheckInRecordRepository
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.event.entity.EventStatus
import com.blockchain2026.team4.backend.event.service.EventService
import com.blockchain2026.team4.backend.resale.service.ResaleService
import com.blockchain2026.team4.backend.ticket.entity.TicketStatus
import com.blockchain2026.team4.backend.ticket.service.TicketService
import com.blockchain2026.team4.backend.user.entity.UserRole
import com.blockchain2026.team4.backend.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

@Service
class CheckInService(
    private val ticketService: TicketService,
    private val userService: UserService,
    private val eventService: EventService,
    private val resaleService: ResaleService,
    private val trustTicketGateway: TrustTicketGateway,
    private val blockchainTransactionService: BlockchainTransactionService,
    private val checkInRecordRepository: CheckInRecordRepository,
    private val checkInRecordMapper: CheckInRecordMapper,
    private val qrCodeService: QrCodeService,
) {
    @Transactional(readOnly = true)
    fun createQr(userId: UUID, command: QrCreateCommand): QrCodeDto {
        val ticket = ticketService.findEntity(command.ticketId)
        if (ticket.owner?.id != userId) throw BusinessException(ErrorCode.FORBIDDEN, "소유한 티켓만 QR을 생성할 수 있습니다.")
        if (ticket.status !in setOf(TicketStatus.SOLD, TicketStatus.LISTED)) throw BusinessException(ErrorCode.CONFLICT, "유효한 티켓만 QR을 생성할 수 있습니다.")
        if (ticket.event.status != EventStatus.PUBLISHED) {
            throw BusinessException(ErrorCode.CONFLICT, "활성 이벤트 티켓만 QR을 생성할 수 있습니다.")
        }

        val tokenId = ticketService.contractTokenId(ticket)
        val payload = """
            {"ticketId":"${command.ticketId}","tokenId":"$tokenId","claimedOwner":"${command.claimedOwner}","expiresAt":"${command.expiresAt.epochSecond}","signature":"${command.signature}"}
        """.trimIndent()
        return QrCodeDto(
            ticketId = command.ticketId,
            contractTokenId = tokenId.toString(),
            payload = payload,
            qrPngBase64 = qrCodeService.createPngBase64(payload),
            barcodeText = tokenId.toString(),
            expiresAt = command.expiresAt,
        )
    }

    @Transactional(readOnly = true)
    fun checkInMessage(ticketId: UUID, claimedOwner: String, expiresAt: Instant): CheckInMessageDto {
        val ticket = ticketService.findEntity(ticketId)
        val tokenId = ticketService.contractTokenId(ticket)
        return CheckInMessageDto(
            ticketId = ticketId,
            contractTokenId = tokenId.toString(),
            claimedOwner = claimedOwner,
            expiresAt = expiresAt,
            messageHash = trustTicketGateway.getTicketCheckInMessageHash(tokenId, claimedOwner, expiresAt.epochSecond.toBigInteger()),
        )
    }

    @Transactional
    fun checkIn(validatorId: UUID, command: CheckInCommand): CheckInRecordDto {
        val validator = userService.findEntity(validatorId)
        val ticket = ticketService.findEntity(command.ticketId)
        if (!eventService.canValidate(ticket.event.id, validatorId)) {
            throw BusinessException(ErrorCode.FORBIDDEN, "해당 이벤트 체크인 검증자 권한이 없습니다.")
        }
        if (ticket.status == TicketStatus.USED) throw BusinessException(ErrorCode.CONFLICT, "이미 사용 완료된 티켓입니다.")
        if (ticket.owner == null || ticket.status !in setOf(TicketStatus.SOLD, TicketStatus.LISTED)) {
            throw BusinessException(ErrorCode.CONFLICT, "판매 완료된 유효 티켓만 체크인할 수 있습니다.")
        }
        if (ticket.event.status != EventStatus.PUBLISHED) {
            throw BusinessException(ErrorCode.CONFLICT, "비활성 이벤트 티켓은 체크인할 수 없습니다.")
        }

        val valid = trustTicketGateway.verifySignedTicket(
            contractTokenId = ticketService.contractTokenId(ticket),
            claimedOwner = command.claimedOwner,
            expiresAtEpochSeconds = command.expiresAt.epochSecond.toBigInteger(),
            signature = command.signature,
        )
        if (!valid) {
            val failed = checkInRecordRepository.save(
                CheckInRecordEntity(ticket = ticket, validator = validator, result = CheckInResult.FAILED, memo = command.memo),
            )
            throw BusinessException(ErrorCode.WALLET_SIGNATURE_INVALID, "QR 서명 또는 티켓 상태가 유효하지 않습니다.", IllegalStateException(failed.id.toString()))
        }

        ticket.contractTokenId?.let {
            val submission = trustTicketGateway.useTicket(it)
            blockchainTransactionService.record(submission)
        }
        resaleService.closeActiveListingForUsedTicket(ticket.id)
        ticketService.markUsed(ticket)
        return checkInRecordMapper.toDto(
            checkInRecordRepository.save(
                CheckInRecordEntity(ticket = ticket, validator = validator, result = CheckInResult.SUCCESS, memo = command.memo),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun history(actorId: UUID, ticketId: UUID): List<CheckInRecordDto> {
        val ticket = ticketService.findEntity(ticketId)
        val actor = userService.getUser(actorId)
        if (!actor.roles.contains(UserRole.ADMIN) &&
            actor.id != ticket.event.organizer.id &&
            !eventService.canValidate(ticket.event.id, actorId)
        ) {
            throw BusinessException(ErrorCode.FORBIDDEN, "체크인 이력 조회 권한이 없습니다.")
        }
        return checkInRecordRepository.findAllByTicketId(ticketId).map(checkInRecordMapper::toDto)
    }
}
