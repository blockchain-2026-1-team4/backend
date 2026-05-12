package com.blockchain2026.team4.backend.user.controller

import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.security.AuthPrincipal
import com.blockchain2026.team4.backend.common.security.CurrentUser
import com.blockchain2026.team4.backend.user.controller.request.UpdateMeRequest
import com.blockchain2026.team4.backend.user.controller.response.UserResponse
import com.blockchain2026.team4.backend.user.entity.UserStatus
import com.blockchain2026.team4.backend.user.facade.UserFacade
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
import java.util.UUID

@Tag(name = "사용자", description = "내 정보와 관리자 사용자 관리 API")
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userFacade: UserFacade,
) {
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 지갑, 이메일, 권한 정보를 조회합니다.")
    @GetMapping("/me")
    fun me(@CurrentUser principal: AuthPrincipal): UserResponse = userFacade.me(principal.userId)

    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 표시 이름 등 기본 프로필을 수정합니다.")
    @PatchMapping("/me")
    fun updateMe(
        @CurrentUser principal: AuthPrincipal,
        @Valid @RequestBody request: UpdateMeRequest,
    ): UserResponse = userFacade.updateMe(principal.userId, request)

    @Operation(summary = "사용자 목록 조회", description = "관리자가 상태별 사용자 목록을 페이지 단위로 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun listUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: UserStatus?,
    ): PageResponse<UserResponse> = userFacade.listUsers(page, size, status)

    @Operation(summary = "사용자 정지", description = "관리자가 사용자를 정지 상태로 변경합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/suspend")
    fun suspend(@PathVariable userId: UUID): UserResponse = userFacade.suspend(userId)

    @Operation(summary = "사용자 활성화", description = "관리자가 정지된 사용자를 다시 활성화합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/activate")
    fun activate(@PathVariable userId: UUID): UserResponse = userFacade.activate(userId)
}
