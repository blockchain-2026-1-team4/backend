package com.blockchain2026.team4.backend.organizer.controller

import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.security.AuthPrincipal
import com.blockchain2026.team4.backend.common.security.CurrentUser
import com.blockchain2026.team4.backend.organizer.controller.request.OrganizerApplicationRequest
import com.blockchain2026.team4.backend.organizer.controller.request.OrganizerReviewRequest
import com.blockchain2026.team4.backend.organizer.controller.response.OrganizerApplicationResponse
import com.blockchain2026.team4.backend.organizer.entity.OrganizerApplicationStatus
import com.blockchain2026.team4.backend.organizer.facade.OrganizerApplicationFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "주최자 승인", description = "주최자 신청과 관리자 승인 API")
@RestController
@RequestMapping("/api/v1/organizer-applications")
class OrganizerApplicationController(
    private val organizerApplicationFacade: OrganizerApplicationFacade,
) {
    @Operation(summary = "주최자 신청", description = "사용자가 이벤트 등록 권한을 얻기 위해 주최자 승인을 신청합니다.")
    @PostMapping
    fun apply(
        @CurrentUser principal: AuthPrincipal,
        @Valid @RequestBody request: OrganizerApplicationRequest,
    ): OrganizerApplicationResponse = organizerApplicationFacade.apply(principal.userId, request)

    @Operation(summary = "내 주최자 신청 조회", description = "현재 사용자의 주최자 신청 이력과 심사 상태를 조회합니다.")
    @GetMapping("/me")
    fun mine(@CurrentUser principal: AuthPrincipal): List<OrganizerApplicationResponse> =
        organizerApplicationFacade.mine(principal.userId)

    @Operation(summary = "주최자 신청 목록", description = "관리자가 주최자 신청 목록을 상태별로 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun list(
        @RequestParam(required = false) status: OrganizerApplicationStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<OrganizerApplicationResponse> = organizerApplicationFacade.list(status, page, size)

    @Operation(summary = "주최자 신청 심사", description = "관리자가 주최자 신청을 승인 또는 거절하고 승인 시 컨트랙트 권한 부여를 요청합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{applicationId}/review")
    fun review(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable applicationId: UUID,
        @Valid @RequestBody request: OrganizerReviewRequest,
    ): OrganizerApplicationResponse = organizerApplicationFacade.review(principal.userId, applicationId, request)
}
