package com.blockchain2026.team4.backend.admin.facade

import com.blockchain2026.team4.backend.admin.controller.response.AdminDashboardResponse
import com.blockchain2026.team4.backend.admin.controller.response.BlockchainTransactionResponse
import com.blockchain2026.team4.backend.admin.service.AdminDashboardService
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import org.springframework.stereotype.Component

@Component
class AdminFacade(
    private val adminDashboardService: AdminDashboardService,
    private val blockchainTransactionService: BlockchainTransactionService,
    private val adminResponseMapper: AdminResponseMapper,
) {
    fun dashboard(): AdminDashboardResponse = adminDashboardService.dashboard()

    fun latestBlockchainTransactions(size: Int): List<BlockchainTransactionResponse> =
        adminResponseMapper.toBlockchainTransactionResponses(blockchainTransactionService.latest(size))
}
