package com.blockchain2026.team4.backend.admin.facade

import com.blockchain2026.team4.backend.admin.controller.response.BlockchainTransactionResponse
import com.blockchain2026.team4.backend.admin.controller.response.ResaleTransactionResponse
import com.blockchain2026.team4.backend.blockchain.dto.BlockchainTransactionDto
import com.blockchain2026.team4.backend.resale.dto.ResaleListingDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface AdminResponseMapper {
    fun toBlockchainTransactionResponse(dto: BlockchainTransactionDto): BlockchainTransactionResponse

    fun toBlockchainTransactionResponses(dtos: List<BlockchainTransactionDto>): List<BlockchainTransactionResponse>

    @Mapping(source = "id", target = "listingId")
    fun toResaleTransactionResponse(dto: ResaleListingDto): ResaleTransactionResponse

    fun toResaleTransactionResponses(dtos: List<ResaleListingDto>): List<ResaleTransactionResponse>
}
