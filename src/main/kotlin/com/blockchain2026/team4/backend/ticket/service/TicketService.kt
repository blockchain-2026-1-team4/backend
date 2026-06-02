package com.blockchain2026.team4.backend.ticket.service

import com.blockchain2026.team4.backend.blockchain.gateway.TrustTicketGateway
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.common.config.AppProperties
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.event.entity.EventStatus
import com.blockchain2026.team4.backend.event.service.EventService
import com.blockchain2026.team4.backend.ticket.dto.TicketDto
import com.blockchain2026.team4.backend.ticket.dto.TicketIssueCommand
import com.blockchain2026.team4.backend.ticket.dto.TicketPurchaseCommand
import com.blockchain2026.team4.backend.ticket.dto.TicketValidityDto
import com.blockchain2026.team4.backend.ticket.entity.TicketEntity
import com.blockchain2026.team4.backend.ticket.entity.TicketStatus
import com.blockchain2026.team4.backend.ticket.mapper.TicketMapper
import com.blockchain2026.team4.backend.ticket.repository.TicketRepository
import com.blockchain2026.team4.backend.user.entity.UserRole
import com.blockchain2026.team4.backend.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

@Service
class TicketService(
    private val ticketRepository: TicketRepository,
    private val eventService: EventService,
    private val userService: UserService,
    private val trustTicketGateway: TrustTicketGateway,
    private val blockchainTransactionService: BlockchainTransactionService,
    private val appProperties: AppProperties,
    private val ticketMapper: TicketMapper,
) {
    @Transactional
    fun issueTickets(organizerId: UUID, eventId: UUID, command: TicketIssueCommand): List<TicketDto> {
        val event = eventService.findEntity(eventId)
        if (event.organizer.id != organizerId) {
            throw BusinessException(ErrorCode.FORBIDDEN, "해당 이벤트의 주최자만 티켓을 발행할 수 있습니다.")
        }
        val existing = ticketRepository.countByEventId(eventId)
        if (existing + command.seatInfos.size > event.totalTicketCount) {
            throw BusinessException(ErrorCode.CONFLICT, "발행 가능한 티켓 수량을 초과했습니다.")
        }

        val saved = command.seatInfos.map { seatInfo ->
            val contractTokenId = event.contractEventId?.let {
                val submission = trustTicketGateway.mintTicket(it, seatInfo)
                blockchainTransactionService.record(submission)
                submission.contractTokenId
            } ?: if (appProperties.blockchain.enabled) {
                throw BusinessException(ErrorCode.CONFLICT, "이벤트의 온체인 eventId가 저장되지 않아 티켓을 발행할 수 없습니다.")
            } else {
                null
            }
            ticketRepository.save(
                TicketEntity(
                    event = event,
                    contractTokenId = contractTokenId,
                    seatInfo = seatInfo,
                    originalPriceWei = event.ticketPriceWei,
                ),
            )
        }
        return ticketMapper.toDtos(saved)
    }

    @Transactional
    fun purchaseTicket(userId: UUID, ticketId: UUID, command: TicketPurchaseCommand = TicketPurchaseCommand()): TicketDto {
        val user = userService.findEntity(userId)
        val ticket = findEntity(ticketId)
        val event = ticket.event
        val now = Instant.now()
        if (event.status != EventStatus.ACTIVE) {
            throw BusinessException(ErrorCode.CONFLICT, "활성 이벤트의 티켓만 구매할 수 있습니다.")
        }
        if (now.isBefore(event.primarySaleStart) || now.isAfter(event.primarySaleEnd)) {
            throw BusinessException(ErrorCode.CONFLICT, "1차 판매 기간이 아닙니다.")
        }
        if (ticket.status != TicketStatus.AVAILABLE) {
            throw BusinessException(ErrorCode.CONFLICT, "구매 가능한 티켓이 아닙니다.")
        }

        val tokenId = ticket.contractTokenId
        if (tokenId == null) {
            if (appProperties.blockchain.enabled) {
                throw BusinessException(ErrorCode.CONFLICT, "온체인 tokenId가 저장되지 않아 구매를 확정할 수 없습니다.")
            }
        } else if (appProperties.blockchain.enabled) {
            val wallet = user.walletAddress?.takeIf { it.isNotBlank() }
                ?: throw BusinessException(ErrorCode.INVALID_REQUEST, "지갑 주소가 있는 사용자만 온체인 티켓을 구매할 수 있습니다.")
            val submission = trustTicketGateway.confirmPrimaryPurchase(
                contractTokenId = tokenId,
                buyerWallet = wallet,
                transactionHash = requireTransactionHash(command.transactionHash),
            )
            blockchainTransactionService.record(submission)
        } else {
            val submission = command.transactionHash
                ?.takeIf { it.isNotBlank() }
                ?.let { trustTicketGateway.confirmPrimaryPurchase(tokenId, user.walletAddress ?: "", it) }
                ?: trustTicketGateway.purchaseTicket(tokenId, ticket.originalPriceWei)
            blockchainTransactionService.record(submission)
        }
        ticket.owner = user
        ticket.status = TicketStatus.SOLD
        eventService.registerPrimarySale(event)
        return ticketMapper.toDto(ticket)
    }

    @Transactional(readOnly = true)
    fun get(ticketId: UUID): TicketDto = ticketMapper.toDto(findEntity(ticketId))

    @Transactional(readOnly = true)
    fun listByEvent(eventId: UUID): List<TicketDto> = ticketMapper.toDtos(ticketRepository.findAllByEventId(eventId))

    @Transactional(readOnly = true)
    fun listMine(userId: UUID): List<TicketDto> = ticketMapper.toDtos(ticketRepository.findAllByOwnerId(userId))

    @Transactional(readOnly = true)
    fun listByOwnerWallet(walletAddress: String): List<TicketDto> =
        ticketMapper.toDtos(ticketRepository.findAllByOwnerWalletAddressIgnoreCase(walletAddress.normalizeWallet()))

    @Transactional(readOnly = true)
    fun validity(ticketId: UUID): TicketValidityDto {
        val ticket = findEntity(ticketId)
        val valid = ticket.event.status == EventStatus.ACTIVE && ticket.owner != null && ticket.status in setOf(TicketStatus.SOLD, TicketStatus.LISTED)
        val reason = when {
            ticket.event.status != EventStatus.ACTIVE -> "이벤트가 비활성 상태입니다."
            ticket.owner == null -> "아직 판매되지 않은 티켓입니다."
            ticket.status == TicketStatus.USED -> "이미 사용 완료된 티켓입니다."
            ticket.status !in setOf(TicketStatus.SOLD, TicketStatus.LISTED) -> "유효한 소유 상태가 아닙니다."
            else -> null
        }
        return TicketValidityDto(ticket.id, contractTokenId(ticket), valid, reason)
    }

    @Transactional
    fun markListed(ticket: TicketEntity): TicketEntity {
        ticket.status = TicketStatus.LISTED
        return ticket
    }

    @Transactional
    fun markSoldFromResale(ticket: TicketEntity, buyerId: UUID): TicketEntity {
        ticket.owner = userService.findEntity(buyerId)
        ticket.status = TicketStatus.SOLD
        return ticket
    }

    @Transactional
    fun markListingCanceled(ticket: TicketEntity): TicketEntity {
        ticket.status = TicketStatus.SOLD
        return ticket
    }

    @Transactional
    fun markUsed(ticket: TicketEntity): TicketEntity {
        ticket.status = TicketStatus.USED
        ticket.usedAt = Instant.now()
        return ticket
    }

    @Transactional(readOnly = true)
    fun requireValidator(userId: UUID) = userService.requireRole(userId, UserRole.VALIDATOR)

    @Transactional(readOnly = true)
    fun findEntity(ticketId: UUID): TicketEntity =
        ticketRepository.findById(ticketId)
            .orElseThrow { BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "티켓을 찾을 수 없습니다.") }

    fun countSold(): Long = ticketRepository.countByStatus(TicketStatus.SOLD)

    fun countUsed(): Long = ticketRepository.countByStatus(TicketStatus.USED)

    fun contractTokenId(ticket: TicketEntity): BigInteger =
        ticket.contractTokenId
            ?: throw BusinessException(ErrorCode.CONFLICT, "온체인 tokenId가 저장되지 않은 티켓입니다.")

    private fun String.normalizeWallet(): String = trim().lowercase()

    private fun requireTransactionHash(transactionHash: String?): String =
        transactionHash?.takeIf { it.isNotBlank() }
            ?: throw BusinessException(ErrorCode.INVALID_REQUEST, "사용자 지갑에서 서명한 트랜잭션 해시가 필요합니다.")
}
