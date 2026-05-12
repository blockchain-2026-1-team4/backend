package com.blockchain2026.team4.backend.admin.controller

import com.blockchain2026.team4.backend.admin.controller.response.AdminDashboardResponse
import com.blockchain2026.team4.backend.admin.controller.response.BlockchainTransactionResponse
import com.blockchain2026.team4.backend.admin.facade.AdminFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자", description = "관리자 대시보드와 운영 현황 API")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminFacade: AdminFacade,
) {
    @Operation(summary = "관리자 대시보드", description = "이벤트, 티켓, 리셀, 체크인 핵심 운영 지표를 조회합니다.")
    @GetMapping("/dashboard")
    fun dashboard(): AdminDashboardResponse = adminFacade.dashboard()

    @Operation(summary = "블록체인 트랜잭션 기록", description = "백엔드가 제출하거나 시뮬레이션한 최근 컨트랙트 트랜잭션 기록을 조회합니다.")
    @GetMapping("/blockchain-transactions")
    fun latestBlockchainTransactions(
        @RequestParam(defaultValue = "20") size: Int,
    ): List<BlockchainTransactionResponse> = adminFacade.latestBlockchainTransactions(size)
}
