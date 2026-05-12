package com.blockchain2026.team4.backend.blockchain.service

import com.blockchain2026.team4.backend.blockchain.dto.BlockchainSubmission
import com.blockchain2026.team4.backend.blockchain.dto.BlockchainTransactionDto
import com.blockchain2026.team4.backend.blockchain.entity.BlockchainTransactionEntity
import com.blockchain2026.team4.backend.blockchain.mapper.BlockchainTransactionMapper
import com.blockchain2026.team4.backend.blockchain.repository.BlockchainTransactionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BlockchainTransactionService(
    private val blockchainTransactionRepository: BlockchainTransactionRepository,
    private val blockchainTransactionMapper: BlockchainTransactionMapper,
) {
    @Transactional
    fun record(submission: BlockchainSubmission): BlockchainTransactionDto =
        blockchainTransactionMapper.toDto(
            blockchainTransactionRepository.save(
                BlockchainTransactionEntity(
                    action = submission.action,
                    transactionHash = submission.transactionHash,
                    status = submission.status,
                    errorMessage = submission.errorMessage,
                ),
            ),
        )

    @Transactional(readOnly = true)
    fun latest(size: Int): List<BlockchainTransactionDto> =
        blockchainTransactionRepository.findAll(PageRequest.of(0, size)).content.map(blockchainTransactionMapper::toDto)
}
