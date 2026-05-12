package com.blockchain2026.team4.backend.auth.controller

import com.blockchain2026.team4.backend.auth.controller.request.EmailLoginRequest
import com.blockchain2026.team4.backend.auth.controller.request.EmailRegisterRequest
import com.blockchain2026.team4.backend.auth.controller.request.WalletLoginRequest
import com.blockchain2026.team4.backend.auth.controller.request.WalletNonceRequest
import com.blockchain2026.team4.backend.auth.controller.response.AuthTokenResponse
import com.blockchain2026.team4.backend.auth.controller.response.WalletNonceResponse
import com.blockchain2026.team4.backend.auth.facade.AuthFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증", description = "지갑 기반 로그인과 보조 이메일 인증 API")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authFacade: AuthFacade,
) {
    @Operation(summary = "지갑 로그인 nonce 발급", description = "프론트가 지갑 서명을 요청하기 전에 일회용 nonce와 서명 메시지를 발급합니다.")
    @PostMapping("/wallet/nonce")
    fun walletNonce(@Valid @RequestBody request: WalletNonceRequest): WalletNonceResponse =
        authFacade.walletNonce(request)

    @Operation(summary = "지갑 로그인", description = "사용자 지갑 서명을 검증하고 서비스 JWT를 발급합니다. 지갑 로그인이 기본 인증 방식입니다.")
    @PostMapping("/wallet/login")
    fun walletLogin(@Valid @RequestBody request: WalletLoginRequest): AuthTokenResponse =
        authFacade.walletLogin(request)

    @Operation(summary = "이메일 회원가입", description = "보조 인증 수단으로 사용할 이메일/비밀번호 계정을 생성하고 JWT를 발급합니다.")
    @PostMapping("/email/register")
    fun emailRegister(@Valid @RequestBody request: EmailRegisterRequest): AuthTokenResponse =
        authFacade.emailRegister(request)

    @Operation(summary = "이메일 로그인", description = "보조 이메일 인증 정보를 검증하고 JWT를 발급합니다.")
    @PostMapping("/email/login")
    fun emailLogin(@Valid @RequestBody request: EmailLoginRequest): AuthTokenResponse =
        authFacade.emailLogin(request)
}
