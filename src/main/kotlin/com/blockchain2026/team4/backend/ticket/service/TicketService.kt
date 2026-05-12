package com.blockchain2026.team4.backend.ticket.service

import com.blockchain2026.team4.backend.blockchain.gateway.TrustTicketGateway
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.event.entity.EventStatus
import com.blockchain2026.team4.backend.event.service.EventService
import com.blockchain2026.team4.backend.ticket.dto.TicketDto
import com.blockchain2026.team4.backend.ticket.dto.TicketIssueCommand
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
            event.contractEventId?.let {
                val submission = trustTicketGateway.mintTicket(it, seatInfo)
                blockchainTransactionService.record(submission)
            }
            ticketRepository.save(
                TicketEntity(
                    event = event,
                    seatInfo = seatInfo,
                    originalPriceWei = event.ticketPriceWei,
                ),
            )
        }
        return ticketMapper.toDtos(saved)
    }

    @Transactional
    fun purchaseTicket(userId: UUID, ticketId: UUID): TicketDto {
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

        ticket.contractTokenId?.let {
            val submission = trustTicketGateway.purchaseTicket(it, ticket.originalPriceWei)
            blockchainTransactionService.record(submission)
        }
        ticket.owner = user
        ticket.status = TicketStatus.SOLD
        return ticketMapper.toDto(ticket)
    }

    @Transactional(readOnly = true)
    fun get(ticketId: UUID): TicketDto = ticketMapper.toDto(findEntity(ticketId))

    @Transactional(readOnly = true)
    fun listByEvent(eventId: UUID): List<TicketDto> = ticketMapper.toDtos(ticketRepository.findAllByEventId(eventId))

    @Transactional(readOnly = true)
    fun listMine(userId: UUID): List<TicketDto> = ticketMapper.toDtos(ticketRepository.findAllByOwnerId(userId))

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
        ticket.contractTokenId ?: BigInteger.valueOf(ticket.id.mostSignificantBits and Long.MAX_VALUE)
}
