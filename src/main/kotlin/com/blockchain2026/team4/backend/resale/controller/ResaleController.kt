package com.blockchain2026.team4.backend.resale.controller

import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.security.AuthPrincipal
import com.blockchain2026.team4.backend.common.security.CurrentUser
import com.blockchain2026.team4.backend.resale.controller.request.ResaleCreateRequest
import com.blockchain2026.team4.backend.resale.controller.response.ResaleListingResponse
import com.blockchain2026.team4.backend.resale.facade.ResaleFacade
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
import java.util.UUID

@Tag(name = "리셀", description = "공식 리셀 등록, 구매, 취소 API")
@RestController
@RequestMapping("/api/v1")
class ResaleController(
    private val resaleFacade: ResaleFacade,
) {
    @Operation(summary = "리셀 목록 조회", description = "공식 리셀 마켓에 등록된 활성 티켓 목록을 조회합니다.")
    @GetMapping("/resale-listings")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<ResaleListingResponse> = resaleFacade.list(page, size)

    @Operation(summary = "리셀 상세 조회", description = "리셀 등록 가격, 판매자, 티켓 정보를 조회합니다.")
    @GetMapping("/resale-listings/{listingId}")
    fun get(@PathVariable listingId: UUID): ResaleListingResponse = resaleFacade.get(listingId)

    @Operation(summary = "리셀 등록", description = "티켓 소유자가 공식 리셀 마켓에 티켓을 등록합니다.")
    @PostMapping("/tickets/{ticketId}/resale-listing")
    fun create(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable ticketId: UUID,
        @Valid @RequestBody request: ResaleCreateRequest,
    ): ResaleListingResponse = resaleFacade.create(principal.userId, ticketId, request)

    @Operation(summary = "리셀 구매", description = "사용자가 활성 리셀 티켓을 구매하고 백엔드가 컨트랙트 리셀 구매 트랜잭션을 제출합니다.")
    @PostMapping("/resale-listings/{listingId}/purchase")
    fun purchase(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable listingId: UUID,
    ): ResaleListingResponse = resaleFacade.purchase(principal.userId, listingId)

    @Operation(summary = "리셀 취소", description = "판매자가 자신의 리셀 등록을 취소합니다.")
    @PatchMapping("/resale-listings/{listingId}/cancel")
    fun cancel(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable listingId: UUID,
    ): ResaleListingResponse = resaleFacade.cancel(principal.userId, listingId)
}
