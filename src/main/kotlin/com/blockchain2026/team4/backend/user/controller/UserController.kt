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

    @Operation(summary = "사용자 검색", description = "이메일 또는 표시 이름으로 사용자를 검색합니다. 주최자 이상 접근 가능합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @GetMapping("/search")
    fun searchUsers(
        @RequestParam query: String,
        @RequestParam(defaultValue = "10") size: Int,
    ): PageResponse<UserResponse> = userFacade.searchUsers(query, size)

    @Operation(summary = "사용자 정지", description = "관리자가 사용자를 정지 상태로 변경합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/suspend")
    fun suspend(@PathVariable userId: UUID): UserResponse = userFacade.suspend(userId)

    @Operation(summary = "사용자 활성화", description = "관리자가 정지된 사용자를 다시 활성화합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/activate")
    fun activate(@PathVariable userId: UUID): UserResponse = userFacade.activate(userId)

    @Operation(summary = "사용자 삭제", description = "관리자가 사용자를 삭제 상태로 전환합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/delete")
    fun delete(@PathVariable userId: UUID): UserResponse = userFacade.delete(userId)

    @Operation(summary = "전역 검증자 권한 부여", description = "관리자가 사용자에게 전체 이벤트 체크인 검증자 권한을 부여하고 컨트랙트 권한 등록을 요청합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/validator")
    fun grantValidator(@PathVariable userId: UUID): UserResponse = userFacade.grantValidator(userId)

    @Operation(summary = "전역 검증자 권한 회수", description = "관리자가 사용자에게서 전체 이벤트 체크인 검증자 권한을 회수합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/validator/revoke")
    fun revokeValidator(@PathVariable userId: UUID): UserResponse = userFacade.revokeValidator(userId)

    @Operation(summary = "주최자 권한 부여", description = "관리자가 사용자에게 주최자 권한을 부여합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/organizer")
    fun grantOrganizer(@PathVariable userId: UUID): UserResponse = userFacade.grantOrganizer(userId)

    @Operation(summary = "주최자 권한 회수", description = "관리자가 사용자에게서 주최자 권한을 회수합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/organizer/revoke")
    fun revokeOrganizer(@PathVariable userId: UUID): UserResponse = userFacade.revokeOrganizer(userId)
}
