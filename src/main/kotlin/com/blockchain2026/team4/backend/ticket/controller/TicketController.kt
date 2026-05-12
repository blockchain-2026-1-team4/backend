package com.blockchain2026.team4.backend.ticket.controller

import com.blockchain2026.team4.backend.common.security.AuthPrincipal
import com.blockchain2026.team4.backend.common.security.CurrentUser
import com.blockchain2026.team4.backend.ticket.controller.request.TicketIssueRequest
import com.blockchain2026.team4.backend.ticket.controller.response.TicketResponse
import com.blockchain2026.team4.backend.ticket.facade.TicketFacade
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

@Tag(name = "티켓", description = "티켓 발행, 구매, 조회 API")
@RestController
@RequestMapping("/api/v1")
class TicketController(
    private val ticketFacade: TicketFacade,
) {
    @Operation(summary = "이벤트 티켓 발행", description = "주최자가 이벤트에 연결된 NFT 티켓 좌석 정보를 일괄 발행합니다.")
    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping("/events/{eventId}/tickets")
    fun issueTickets(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable eventId: UUID,
        @Valid @RequestBody request: TicketIssueRequest,
    ): List<TicketResponse> = ticketFacade.issueTickets(principal.userId, eventId, request)

    @Operation(summary = "이벤트 티켓 목록", description = "특정 이벤트에 발행된 티켓 목록과 현재 판매 상태를 조회합니다.")
    @GetMapping("/events/{eventId}/tickets")
    fun listByEvent(@PathVariable eventId: UUID): List<TicketResponse> = ticketFacade.listByEvent(eventId)

    @Operation(summary = "내 티켓 목록", description = "현재 로그인한 사용자가 보유한 티켓 목록을 조회합니다.")
    @GetMapping("/tickets/me")
    fun mine(@CurrentUser principal: AuthPrincipal): List<TicketResponse> = ticketFacade.listMine(principal.userId)

    @Operation(summary = "티켓 상세 조회", description = "티켓 좌석, 소유자, 온체인 tokenId, 사용 상태를 조회합니다.")
    @GetMapping("/tickets/{ticketId}")
    fun get(@PathVariable ticketId: UUID): TicketResponse = ticketFacade.get(ticketId)

    @Operation(summary = "1차 티켓 구매", description = "사용자가 1차 판매 티켓을 구매하고 백엔드가 컨트랙트 구매 트랜잭션을 제출합니다.")
    @PostMapping("/tickets/{ticketId}/purchase")
    fun purchase(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable ticketId: UUID,
    ): TicketResponse = ticketFacade.purchase(principal.userId, ticketId)
}
