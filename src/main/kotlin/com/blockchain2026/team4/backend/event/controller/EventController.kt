package com.blockchain2026.team4.backend.event.controller

import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.security.AuthPrincipal
import com.blockchain2026.team4.backend.common.security.CurrentUser
import com.blockchain2026.team4.backend.event.controller.request.EventCreateRequest
import com.blockchain2026.team4.backend.event.controller.request.EventStatusRequest
import com.blockchain2026.team4.backend.event.controller.request.EventUpdateRequest
import com.blockchain2026.team4.backend.event.controller.response.EventResponse
import com.blockchain2026.team4.backend.event.entity.EventStatus
import com.blockchain2026.team4.backend.event.facade.EventFacade
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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Tag(name = "이벤트", description = "이벤트 등록, 조회, 수정, 판매 정책 관리 API")
@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val eventFacade: EventFacade,
) {
    @Operation(summary = "이벤트 목록 조회", description = "활성 상태 등 조건에 맞는 이벤트 목록을 페이지 단위로 조회합니다.")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: EventStatus?,
    ): PageResponse<EventResponse> = eventFacade.list(page, size, status)

    @Operation(summary = "이벤트 상세 조회", description = "이벤트 메타데이터와 판매/리셀 정책을 조회합니다.")
    @GetMapping("/{eventId}")
    fun get(@PathVariable eventId: UUID): EventResponse = eventFacade.get(eventId)

    @Operation(summary = "내 이벤트 조회", description = "주최자가 자신이 등록한 이벤트 목록을 조회합니다.")
    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/me")
    fun mine(
        @CurrentUser principal: AuthPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<EventResponse> = eventFacade.listMine(principal.userId, page, size)

    @Operation(summary = "이벤트 등록", description = "주최자가 이벤트와 1차 판매/리셀 정책을 등록하고 컨트랙트 트랜잭션을 요청합니다.")
    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping
    fun create(
        @CurrentUser principal: AuthPrincipal,
        @Valid @RequestBody request: EventCreateRequest,
    ): EventResponse = eventFacade.create(principal.userId, request)

    @Operation(summary = "이벤트 수정", description = "주최자가 오프체인 이벤트 메타데이터를 수정합니다.")
    @PreAuthorize("hasRole('ORGANIZER')")
    @PatchMapping("/{eventId}")
    fun update(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable eventId: UUID,
        @Valid @RequestBody request: EventUpdateRequest,
    ): EventResponse = eventFacade.update(principal.userId, eventId, request)

    @Operation(summary = "이벤트 상태 변경", description = "주최자 또는 관리자가 이벤트 활성/비활성 상태를 변경합니다.")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    @PatchMapping("/{eventId}/status")
    fun changeStatus(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable eventId: UUID,
        @Valid @RequestBody request: EventStatusRequest,
    ): EventResponse = eventFacade.changeStatus(principal.userId, eventId, request)

    @Operation(summary = "이벤트 이미지 업로드", description = "로컬 이미지 디렉터리에 이벤트 이미지를 저장하고 이벤트 이미지 URL을 갱신합니다.")
    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping("/{eventId}/image")
    fun uploadImage(
        @CurrentUser principal: AuthPrincipal,
        @PathVariable eventId: UUID,
        @RequestPart("file") file: MultipartFile,
    ): EventResponse = eventFacade.uploadImage(principal.userId, eventId, file)
}
