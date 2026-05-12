package com.blockchain2026.team4.backend.blockchain.dto

import com.blockchain2026.team4.backend.blockchain.entity.BlockchainTransactionStatus
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

data class BlockchainTransactionDto(
    val id: UUID,
    val action: String,
    val transactionHash: String?,
    val status: BlockchainTransactionStatus,
    val errorMessage: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class BlockchainSubmission(
    val action: String,
    val transactionHash: String?,
    val status: BlockchainTransactionStatus,
    val errorMessage: String? = null,
)

data class ContractEventCommand(
    val eventName: String,
    val eventTimestamp: BigInteger,
    val ticketPriceWei: BigInteger,
    val totalTicketCount: BigInteger,
    val primarySaleStart: BigInteger,
    val primarySaleEnd: BigInteger,
    val resaleAllowed: Boolean,
    val maxResalePriceRate: BigInteger,
    val resaleStart: BigInteger,
    val resaleEnd: BigInteger,
)
