package com.blockchain2026.team4.backend.blockchain.mapper

import com.blockchain2026.team4.backend.blockchain.dto.BlockchainTransactionDto
import com.blockchain2026.team4.backend.blockchain.entity.BlockchainTransactionEntity
import org.mapstruct.Mapper

@Mapper
interface BlockchainTransactionMapper {
    fun toDto(entity: BlockchainTransactionEntity): BlockchainTransactionDto
}
