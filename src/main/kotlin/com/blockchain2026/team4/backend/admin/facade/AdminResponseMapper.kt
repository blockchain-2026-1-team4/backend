package com.blockchain2026.team4.backend.admin.facade

import com.blockchain2026.team4.backend.admin.controller.response.BlockchainTransactionResponse
import com.blockchain2026.team4.backend.blockchain.dto.BlockchainTransactionDto
import org.mapstruct.Mapper

@Mapper
interface AdminResponseMapper {
    fun toBlockchainTransactionResponse(dto: BlockchainTransactionDto): BlockchainTransactionResponse

    fun toBlockchainTransactionResponses(dtos: List<BlockchainTransactionDto>): List<BlockchainTransactionResponse>
}
