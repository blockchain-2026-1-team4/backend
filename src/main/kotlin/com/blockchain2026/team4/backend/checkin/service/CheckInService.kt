package com.blockchain2026.team4.backend.checkin.service

import com.blockchain2026.team4.backend.blockchain.gateway.TrustTicketGateway
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.checkin.dto.CheckInCommand
import com.blockchain2026.team4.backend.checkin.dto.CheckInRecordDto
import com.blockchain2026.team4.backend.checkin.dto.QrCodeDto
import com.blockchain2026.team4.backend.checkin.dto.QrCreateCommand
import com.blockchain2026.team4.backend.checkin.entity.CheckInRecordEntity
import com.blockchain2026.team4.backend.checkin.entity.CheckInResult
import com.blockchain2026.team4.backend.checkin.mapper.CheckInRecordMapper
import com.blockchain2026.team4.backend.checkin.repository.CheckInRecordRepository
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.ticket.entity.TicketStatus
import com.blockchain2026.team4.backend.ticket.service.TicketService
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

        val payload = """
            {"ticketId":"${command.ticketId}","tokenId":"${ticketService.contractTokenId(ticket)}","claimedOwner":"${command.claimedOwner}","expiresAt":"${command.expiresAt.epochSecond}","signature":"${command.signature}"}
        """.trimIndent()
        return QrCodeDto(
            ticketId = command.ticketId,
            payload = payload,
            qrPngBase64 = qrCodeService.createPngBase64(payload),
            expiresAt = command.expiresAt,
        )
    }

    @Transactional
    fun checkIn(validatorId: UUID, command: CheckInCommand): CheckInRecordDto {
        val validator = userService.findEntity(ticketService.requireValidator(validatorId).id)
        val ticket = ticketService.findEntity(command.ticketId)
        if (ticket.status == TicketStatus.USED) throw BusinessException(ErrorCode.CONFLICT, "이미 사용 완료된 티켓입니다.")

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
        ticketService.markUsed(ticket)
        return checkInRecordMapper.toDto(
            checkInRecordRepository.save(
                CheckInRecordEntity(ticket = ticket, validator = validator, result = CheckInResult.SUCCESS, memo = command.memo),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun history(ticketId: UUID): List<CheckInRecordDto> =
        checkInRecordRepository.findAllByTicketId(ticketId).map(checkInRecordMapper::toDto)
}
