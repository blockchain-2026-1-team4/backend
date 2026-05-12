package com.blockchain2026.team4.backend.checkin.controller

import com.blockchain2026.team4.backend.checkin.controller.request.CheckInRequest
import com.blockchain2026.team4.backend.checkin.controller.request.QrCreateRequest
import com.blockchain2026.team4.backend.checkin.controller.response.CheckInRecordResponse
import com.blockchain2026.team4.backend.checkin.controller.response.QrCodeResponse
import com.blockchain2026.team4.backend.checkin.facade.CheckInFacade
import com.blockchain2026.team4.backend.common.security.AuthPrincipal
import com.blockchain2026.team4.backend.common.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "체크인", description = "QR 생성, 검증, 입장 처리 API")
@RestController
@RequestMapping("/api/v1")
class CheckInController(
    private val checkInFacade: CheckInFacade,
) {
    @Operation(summary = "티켓 QR 생성", description = "티켓 소유자가 제출한 서명 payload로 백엔드가 QR PNG 이미지를 생성합니다.")
    @PostMapping("/tickets/{ticketId}/qr")
    fun createQr(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable ticketId: UUID,
        @Valid @RequestBody request: QrCreateRequest,
    ): QrCodeResponse = checkInFacade.createQr(principal.userId, ticketId, request)

    @Operation(summary = "입장 처리", description = "검증자가 QR 서명 유효성을 확인하고 컨트랙트 useTicket 트랜잭션으로 티켓 사용 완료를 처리합니다.")
    @PreAuthorize("hasRole('VALIDATOR')")
    @PostMapping("/check-ins")
    fun checkIn(
        @CurrentUser principal: AuthPrincipal,
        @Valid @RequestBody request: CheckInRequest,
    ): CheckInRecordResponse = checkInFacade.checkIn(principal.userId, request)

    @Operation(summary = "체크인 이력 조회", description = "특정 티켓의 체크인 시도와 성공 이력을 조회합니다.")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN','VALIDATOR')")
    @GetMapping("/tickets/{ticketId}/check-ins")
    fun history(@PathVariable ticketId: UUID): List<CheckInRecordResponse> = checkInFacade.history(ticketId)
}
