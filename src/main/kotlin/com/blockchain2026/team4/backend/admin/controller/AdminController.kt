package com.blockchain2026.team4.backend.admin.controller

import com.blockchain2026.team4.backend.admin.controller.response.AdminDashboardResponse
import com.blockchain2026.team4.backend.admin.controller.response.BlockchainTransactionResponse
import com.blockchain2026.team4.backend.admin.controller.response.ResaleTransactionResponse
import com.blockchain2026.team4.backend.admin.facade.AdminFacade
import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.security.AuthPrincipal
import com.blockchain2026.team4.backend.common.security.CurrentUser
import com.blockchain2026.team4.backend.dispute.controller.request.DisputeReviewRequest
import com.blockchain2026.team4.backend.dispute.controller.response.DisputeResponse
import com.blockchain2026.team4.backend.dispute.entity.DisputeStatus
import com.blockchain2026.team4.backend.event.controller.response.EventResponse
import com.blockchain2026.team4.backend.event.entity.EventStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import com.blockchain2026.team4.backend.resale.entity.ResaleListingStatus
import java.util.UUID

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

    @Operation(summary = "관리자 이벤트 감독", description = "관리자가 이벤트를 검색하고 상태, 카테고리, 플래그 여부로 조회합니다.")
    @GetMapping("/events")
    fun events(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: EventStatus?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) flagged: Boolean?,
    ): PageResponse<EventResponse> = adminFacade.listEvents(page, size, status, category, query, flagged)

    @Operation(summary = "이벤트 플래그 처리", description = "관리자가 이상 이벤트를 플래그 처리해 감독 대상임을 표시합니다.")
    @PatchMapping("/events/{eventId}/flag")
    fun flagEvent(@PathVariable eventId: UUID): EventResponse = adminFacade.flagEvent(eventId)

    @Operation(summary = "이벤트 플래그 해제", description = "관리자가 이벤트 플래그를 해제합니다.")
    @PatchMapping("/events/{eventId}/unflag")
    fun unflagEvent(@PathVariable eventId: UUID): EventResponse = adminFacade.unflagEvent(eventId)

    @Operation(summary = "리셀 거래 내역", description = "관리자가 완료 또는 취소된 리셀 거래 기록을 조회합니다.")
    @GetMapping("/resale-transactions")
    fun resaleTransactions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: ResaleListingStatus?,
    ): PageResponse<ResaleTransactionResponse> = adminFacade.resaleTransactions(page, size, status)

    @Operation(summary = "분쟁 신고 목록", description = "관리자가 접수된 분쟁 신고를 상태별로 조회합니다.")
    @GetMapping("/disputes")
    fun disputes(
        @RequestParam(required = false) status: DisputeStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<DisputeResponse> = adminFacade.disputes(status, page, size)

    @Operation(summary = "분쟁 신고 처리", description = "관리자가 분쟁 신고 상태와 처리 메모를 업데이트합니다.")
    @PatchMapping("/disputes/{disputeId}/review")
    fun reviewDispute(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable disputeId: UUID,
        @Valid @RequestBody request: DisputeReviewRequest,
    ): DisputeResponse = adminFacade.reviewDispute(principal.userId, disputeId, request)
}
