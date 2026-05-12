package com.blockchain2026.team4.backend.admin.controller.response

import com.blockchain2026.team4.backend.blockchain.entity.BlockchainTransactionStatus
import java.time.Instant
import java.util.UUID

data class BlockchainTransactionResponse(
    val id: UUID,
    val action: String,
    val transactionHash: String?,
    val status: BlockchainTransactionStatus,
    val errorMessage: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
