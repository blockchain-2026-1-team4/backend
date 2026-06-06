package com.blockchain2026.team4.backend.dispute.controller

import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.security.AuthPrincipal
import com.blockchain2026.team4.backend.common.security.CurrentUser
import com.blockchain2026.team4.backend.dispute.controller.request.DisputeCreateRequest
import com.blockchain2026.team4.backend.dispute.controller.request.DisputeUpdateRequest
import com.blockchain2026.team4.backend.dispute.controller.response.DisputeResponse
import com.blockchain2026.team4.backend.dispute.facade.DisputeFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "분쟁", description = "리셀 거래 분쟁 신고와 내 신고 이력 API")
@RestController
@RequestMapping("/api/v1/disputes")
class DisputeController(
    private val disputeFacade: DisputeFacade,
) {
    @Operation(summary = "분쟁 신고", description = "사용자가 리셀 거래 또는 티켓 관련 분쟁을 신고합니다.")
    @PostMapping
    fun create(
        @CurrentUser principal: AuthPrincipal,
        @Valid @RequestBody request: DisputeCreateRequest,
    ): DisputeResponse = disputeFacade.create(principal.userId, request)

    @Operation(summary = "내 분쟁 신고 조회", description = "현재 사용자가 접수한 분쟁 신고 이력을 조회합니다.")
    @GetMapping("/me")
    fun mine(
        @CurrentUser principal: AuthPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<DisputeResponse> = disputeFacade.listMine(principal.userId, page, size)

    @Operation(summary = "분쟁 신고 수정", description = "접수 단계의 분쟁 신고 사유와 내용을 수정합니다.")
    @PatchMapping("/{disputeId}")
    fun update(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable disputeId: java.util.UUID,
        @Valid @RequestBody request: DisputeUpdateRequest,
    ): DisputeResponse = disputeFacade.update(principal.userId, disputeId, request)

    @Operation(summary = "분쟁 신고 취소", description = "접수 단계의 본인 분쟁 신고를 취소합니다.")
    @PatchMapping("/{disputeId}/cancel")
    fun cancel(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable disputeId: java.util.UUID,
    ): DisputeResponse = disputeFacade.cancel(principal.userId, disputeId)
}
