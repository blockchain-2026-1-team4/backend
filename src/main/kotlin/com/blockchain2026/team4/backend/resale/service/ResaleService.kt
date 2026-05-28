package com.blockchain2026.team4.backend.resale.service

import com.blockchain2026.team4.backend.blockchain.gateway.TrustTicketGateway
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.event.entity.EventStatus
import com.blockchain2026.team4.backend.resale.dto.ResaleCreateCommand
import com.blockchain2026.team4.backend.resale.dto.ResaleListingDto
import com.blockchain2026.team4.backend.resale.entity.ResaleListingEntity
import com.blockchain2026.team4.backend.resale.entity.ResaleListingStatus
import com.blockchain2026.team4.backend.resale.mapper.ResaleListingMapper
import com.blockchain2026.team4.backend.resale.repository.ResaleListingRepository
import com.blockchain2026.team4.backend.ticket.entity.TicketStatus
import com.blockchain2026.team4.backend.ticket.service.TicketService
import com.blockchain2026.team4.backend.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

@Service
class ResaleService(
    private val resaleListingRepository: ResaleListingRepository,
    private val ticketService: TicketService,
    private val userService: UserService,
    private val trustTicketGateway: TrustTicketGateway,
    private val blockchainTransactionService: BlockchainTransactionService,
    private val resaleListingMapper: ResaleListingMapper,
) {
    @Transactional
    fun createListing(userId: UUID, ticketId: UUID, command: ResaleCreateCommand): ResaleListingDto {
        val seller = userService.findEntity(userId)
        val ticket = ticketService.findEntity(ticketId)
        val event = ticket.event
        val now = Instant.now()

        if (ticket.owner?.id != userId) throw BusinessException(ErrorCode.FORBIDDEN, "소유한 티켓만 리셀 등록할 수 있습니다.")
        if (event.status != EventStatus.PUBLISHED) throw BusinessException(ErrorCode.CONFLICT, "활성 이벤트 티켓만 리셀 등록할 수 있습니다.")
        if (ticket.status != TicketStatus.SOLD) throw BusinessException(ErrorCode.CONFLICT, "판매된 미사용 티켓만 리셀 등록할 수 있습니다.")
        if (!ticket.resaleEnabled) throw BusinessException(ErrorCode.CONFLICT, "이 티켓 구역은 리셀을 허용하지 않습니다.")
        if (event.resaleStart == null || event.resaleEnd == null || now.isBefore(event.resaleStart) || now.isAfter(event.resaleEnd)) {
            throw BusinessException(ErrorCode.CONFLICT, "리셀 가능 기간이 아닙니다.")
        }
        val maxPrice = ticket.originalPriceWei.multiply(ticket.resaleCapRate.toBigInteger()).divide(BigInteger.valueOf(10_000))
        if (command.priceWei > maxPrice) throw BusinessException(ErrorCode.INVALID_REQUEST, "리셀 가격 상한을 초과했습니다.")

        ticket.contractTokenId?.let {
            val submission = trustTicketGateway.listTicket(it, command.priceWei)
            blockchainTransactionService.record(submission)
        }
        ticketService.markListed(ticket)
        return resaleListingMapper.toDto(
            resaleListingRepository.save(
                ResaleListingEntity(ticket = ticket, seller = seller, priceWei = command.priceWei),
            ),
        )
    }

    @Transactional
    fun purchaseListing(userId: UUID, listingId: UUID): ResaleListingDto {
        val buyer = userService.findEntity(userId)
        val listing = findEntity(listingId)
        val event = listing.ticket.event
        val now = Instant.now()
        if (buyer.walletAddress.isNullOrBlank()) {
            throw BusinessException(ErrorCode.FORBIDDEN, "지갑 로그인 후 리셀 티켓을 구매할 수 있습니다.")
        }
        if (listing.status != ResaleListingStatus.ACTIVE) throw BusinessException(ErrorCode.CONFLICT, "활성 리셀 등록이 아닙니다.")
        if (listing.seller.id == userId) throw BusinessException(ErrorCode.INVALID_REQUEST, "본인 티켓은 구매할 수 없습니다.")
        if (event.status != EventStatus.PUBLISHED) throw BusinessException(ErrorCode.CONFLICT, "활성 이벤트의 리셀 티켓만 구매할 수 있습니다.")
        if (event.resaleStart == null || event.resaleEnd == null || now.isBefore(event.resaleStart) || now.isAfter(event.resaleEnd)) {
            throw BusinessException(ErrorCode.CONFLICT, "리셀 가능 기간이 아닙니다.")
        }
        if (listing.ticket.status != TicketStatus.LISTED) throw BusinessException(ErrorCode.CONFLICT, "리셀 등록 중인 티켓이 아닙니다.")
        if (listing.ticket.owner?.id != listing.seller.id) throw BusinessException(ErrorCode.CONFLICT, "현재 티켓 소유자와 판매자가 일치하지 않습니다.")
        listing.ticket.contractTokenId?.let {
            val submission = trustTicketGateway.purchaseResaleTicket(it, listing.priceWei)
            blockchainTransactionService.record(submission)
        }
        ticketService.markSoldFromResale(listing.ticket, userId)
        listing.status = ResaleListingStatus.SOLD
        listing.buyer = buyer
        listing.purchasedAt = now
        return resaleListingMapper.toDto(listing)
    }

    @Transactional
    fun cancelListing(userId: UUID, listingId: UUID): ResaleListingDto {
        val listing = findEntity(listingId)
        if (listing.seller.id != userId) throw BusinessException(ErrorCode.FORBIDDEN, "판매자만 리셀 등록을 취소할 수 있습니다.")
        if (listing.status != ResaleListingStatus.ACTIVE) throw BusinessException(ErrorCode.CONFLICT, "활성 리셀 등록이 아닙니다.")
        listing.ticket.contractTokenId?.let {
            val submission = trustTicketGateway.cancelListing(it)
            blockchainTransactionService.record(submission)
        }
        ticketService.markListingCanceled(listing.ticket)
        listing.status = ResaleListingStatus.CANCELED
        return resaleListingMapper.toDto(listing)
    }

    @Transactional(readOnly = true)
    fun get(listingId: UUID): ResaleListingDto = resaleListingMapper.toDto(findEntity(listingId))

    @Transactional(readOnly = true)
    fun list(page: Int, size: Int): PageResponse<ResaleListingDto> {
        val listings = resaleListingRepository.findAllByStatus(ResaleListingStatus.ACTIVE, PageRequest.of(page, size))
        return PageResponse(
            items = listings.content.map(resaleListingMapper::toDto),
            page = listings.number,
            size = listings.size,
            totalElements = listings.totalElements,
            totalPages = listings.totalPages,
            hasNext = listings.hasNext(),
        )
    }

    fun countActive(): Long = resaleListingRepository.countByStatus(ResaleListingStatus.ACTIVE)

    @Transactional(readOnly = true)
    fun listTransactions(page: Int, size: Int, status: ResaleListingStatus?): PageResponse<ResaleListingDto> {
        val pageable = PageRequest.of(page, size)
        val listings = status?.let { resaleListingRepository.findAllByStatus(it, pageable) }
            ?: resaleListingRepository.findAllByStatusNot(ResaleListingStatus.ACTIVE, pageable)
        return PageResponse(
            items = listings.content.map(resaleListingMapper::toDto),
            page = listings.number,
            size = listings.size,
            totalElements = listings.totalElements,
            totalPages = listings.totalPages,
            hasNext = listings.hasNext(),
        )
    }

    @Transactional
    fun closeActiveListingForUsedTicket(ticketId: UUID) {
        val listing = resaleListingRepository.findByTicketIdAndStatus(ticketId, ResaleListingStatus.ACTIVE) ?: return
        listing.status = ResaleListingStatus.CANCELED
    }

    fun findEntity(listingId: UUID): ResaleListingEntity =
        resaleListingRepository.findById(listingId)
            .orElseThrow { BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "리셀 등록을 찾을 수 없습니다.") }
}
