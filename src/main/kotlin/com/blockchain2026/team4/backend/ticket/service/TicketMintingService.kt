package com.blockchain2026.team4.backend.ticket.service

import com.blockchain2026.team4.backend.blockchain.gateway.TrustTicketGateway
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.ticket.repository.TicketRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.util.UUID

@Service
class TicketMintingService(
    private val ticketRepository: TicketRepository,
    private val trustTicketGateway: TrustTicketGateway,
    private val blockchainTransactionService: BlockchainTransactionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("blockchainMintExecutor")
    fun mintAllAsync(items: List<Pair<UUID, String>>, contractEventId: BigInteger) {
        log.info("[AsyncMint] 민팅 시작: {}개, contractEventId={}", items.size, contractEventId)
        items.forEach { (ticketId, seatInfo) ->
            try {
                val submission = trustTicketGateway.mintTicket(contractEventId, seatInfo)
                blockchainTransactionService.record(submission)
                val tokenId = submission.contractTokenId
                if (tokenId != null) {
                    saveTokenId(ticketId, tokenId)
                    log.info("[AsyncMint] 완료: ticketId={}, tokenId={}", ticketId, tokenId)
                } else {
                    log.warn("[AsyncMint] tokenId 없음: ticketId={}", ticketId)
                }
            } catch (e: Exception) {
                log.error("[AsyncMint] 실패: ticketId={}, seatInfo={}, error={}", ticketId, seatInfo, e.message)
            }
        }
        log.info("[AsyncMint] 전체 완료: {}개", items.size)
    }

    @Transactional
    fun saveTokenId(ticketId: UUID, contractTokenId: BigInteger) {
        ticketRepository.findById(ticketId).ifPresent {
            it.contractTokenId = contractTokenId
            ticketRepository.saveAndFlush(it)
        }
    }
}
