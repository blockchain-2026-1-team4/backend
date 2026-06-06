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
        command.ticketSections.forEach { section ->
            if (section.sectionName.isBlank() || section.priceWei <= BigInteger.ZERO) {
                throw BusinessException(ErrorCode.INVALID_REQUEST, "좌석 구역과 가격을 확인해주세요.")
            }
            if (section.quantity <= 0 || section.startNumber <= 0) {
                throw BusinessException(ErrorCode.INVALID_REQUEST, "발행 수량과 시작 번호를 확인해주세요.")
            }
            val saleStart = section.saleStartAt ?: event.primarySaleStart
            val saleEnd = section.saleEndAt ?: event.primarySaleEnd
            if (!saleStart.isBefore(saleEnd)) {
                throw BusinessException(ErrorCode.INVALID_REQUEST, "${section.sectionName} 판매 종료 시간은 판매 시작 시간보다 늦어야 합니다.")
            }
        }
        val issueItems = command.ticketSections.flatMap { section ->
            (0 until section.quantity).map { offset ->
                val seatInfo = "${section.sectionName}-${section.startNumber + offset}"
                Triple(seatInfo, section.sectionName, section)
            }
        } + command.seatInfos.map { seatInfo ->
            Triple(seatInfo, seatInfo.substringBefore("-").ifBlank { "GENERAL" }, null)
        }
        if (issueItems.isEmpty()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "발행할 티켓 정보가 필요합니다.")
        }
        val duplicatedInRequest = issueItems
            .groupingBy { it.first }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicatedInRequest.isNotEmpty()) {
            throw BusinessException(ErrorCode.CONFLICT, "중복된 좌석 번호가 있습니다: ${duplicatedInRequest.first()}")
        }
        val existingSeatInfos = ticketRepository.findAllByEventId(eventId).map { it.seatInfo }.toSet()
        val duplicatedExisting = issueItems.map { it.first }.firstOrNull { it in existingSeatInfos }
        if (duplicatedExisting != null) {
            throw BusinessException(ErrorCode.CONFLICT, "이미 발행된 좌석 번호입니다: $duplicatedExisting")
        }

        val existing = ticketRepository.countByEventId(eventId)
        command.totalTicketCount?.takeIf { it > event.totalTicketCount }?.let {
            event.totalTicketCount = it
            event.remainingTicketCount = (it - event.soldTicketCount).coerceAtLeast(0)
        }
        if (event.totalTicketCount > 0 && existing + issueItems.size > event.totalTicketCount) {
            throw BusinessException(ErrorCode.CONFLICT, "발행 가능한 티켓 수량을 초과했습니다.")
        }
        val contractEventId = event.contractEventId
            ?: throw BusinessException(ErrorCode.BLOCKCHAIN_TRANSACTION_FAILED, "컨트랙트 이벤트 ID가 없는 이벤트입니다. 이벤트 생성 상태를 확인해주세요.")

        val saved = issueItems.map { (seatInfo, sectionName, sectionPolicy) ->
            val submission = trustTicketGateway.mintTicket(contractEventId, seatInfo)
            blockchainTransactionService.record(submission)
            val contractTokenId = submission.contractTokenId
                ?: throw BusinessException(ErrorCode.BLOCKCHAIN_TRANSACTION_FAILED, "민팅된 티켓의 컨트랙트 토큰 ID를 확인할 수 없습니다.")
            ticketRepository.save(
                TicketEntity(
                    event = event,
                    contractTokenId = contractTokenId,
                    seatInfo = seatInfo,
                    sectionName = sectionName,
                    eventRoundId = sectionPolicy?.eventRoundId,
                    originalPriceWei = sectionPolicy?.priceWei ?: event.ticketPriceWei,
                    saleStartAt = sectionPolicy?.saleStartAt,
                    saleEndAt = sectionPolicy?.saleEndAt,
                    resaleEnabled = sectionPolicy?.resaleEnabled ?: event.resaleAllowed,
                    resaleCapRate = sectionPolicy?.resaleCapRate ?: event.maxResalePriceRate,
                ),
            )
        }

        if (command.ticketSections.any { it.resaleEnabled }) {
            event.resaleAllowed = true
        }

        return ticketMapper.toDtos(saved)
    }

    @Transactional
    fun cancelIssuedTickets(organizerId: UUID, eventId: UUID, ticketIds: List<UUID>): List<TicketDto> {
        val event = eventService.findEntity(eventId)
        if (event.organizer.id != organizerId) {
            throw BusinessException(ErrorCode.FORBIDDEN, "해당 이벤트의 주최자만 티켓 발행을 취소할 수 있습니다.")
        }
        if (ticketIds.isEmpty()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "취소할 티켓 정보가 필요합니다.")
        }

        val tickets = ticketRepository.findAllById(ticketIds)
        if (tickets.size != ticketIds.toSet().size) {
            throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "취소할 티켓을 찾을 수 없습니다.")
        }
        val invalid = tickets.firstOrNull {
            it.event.id != eventId || it.status != TicketStatus.AVAILABLE || it.owner != null
        }
        if (invalid != null) {
            throw BusinessException(ErrorCode.CONFLICT, "판매되었거나 다른 이벤트의 티켓은 발행 취소할 수 없습니다.")
        }

        val canceled = ticketMapper.toDtos(tickets)
        ticketRepository.deleteAll(tickets)
        return canceled
    }

    @Transactional
    fun purchaseTicket(userId: UUID, ticketId: UUID, command: TicketPurchaseCommand = TicketPurchaseCommand()): TicketDto {
        val user = userService.findEntity(userId)
        val ticket = findEntity(ticketId)
        val event = ticket.event
        val now = Instant.now()
        if (user.walletAddress.isNullOrBlank()) {
            throw BusinessException(ErrorCode.FORBIDDEN, "지갑 로그인 후 티켓을 구매할 수 있습니다.")
        }
        if (event.status != EventStatus.PUBLISHED) {
            throw BusinessException(ErrorCode.CONFLICT, "활성 이벤트의 티켓만 구매할 수 있습니다.")
        }
        val saleStart = ticket.saleStartAt ?: event.primarySaleStart
        val saleEnd = ticket.saleEndAt ?: event.primarySaleEnd
        if (now.isBefore(saleStart) || now.isAfter(saleEnd)) {
            throw BusinessException(ErrorCode.CONFLICT, "이 티켓의 판매 기간이 아닙니다.")
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
        val valid = ticket.event.status == EventStatus.PUBLISHED && ticket.owner != null && ticket.status in setOf(TicketStatus.SOLD, TicketStatus.LISTED)
        val reason = when {
            ticket.event.status != EventStatus.PUBLISHED -> "이벤트가 비활성 상태입니다."
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
