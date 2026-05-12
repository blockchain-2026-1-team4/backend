package com.blockchain2026.team4.backend.auth.facade

import com.blockchain2026.team4.backend.auth.controller.request.EmailLoginRequest
import com.blockchain2026.team4.backend.auth.controller.request.EmailRegisterRequest
import com.blockchain2026.team4.backend.auth.controller.request.WalletLoginRequest
import com.blockchain2026.team4.backend.auth.controller.request.WalletNonceRequest
import com.blockchain2026.team4.backend.auth.controller.response.AuthTokenResponse
import com.blockchain2026.team4.backend.auth.controller.response.WalletNonceResponse
import com.blockchain2026.team4.backend.auth.dto.EmailLoginCommand
import com.blockchain2026.team4.backend.auth.dto.EmailRegisterCommand
import com.blockchain2026.team4.backend.auth.dto.WalletLoginCommand
import com.blockchain2026.team4.backend.auth.mapper.AuthApiMapper
import com.blockchain2026.team4.backend.auth.service.AuthService
import org.springframework.stereotype.Component

@Component
class AuthFacade(
    private val authService: AuthService,
    private val authApiMapper: AuthApiMapper,
) {
    fun walletNonce(request: WalletNonceRequest): WalletNonceResponse =
        authApiMapper.toResponse(authService.issueWalletNonce(request.walletAddress))

    fun walletLogin(request: WalletLoginRequest): AuthTokenResponse =
        authApiMapper.toResponse(
            authService.walletLogin(
                WalletLoginCommand(
                    walletAddress = request.walletAddress,
                    nonce = request.nonce,
                    signature = request.signature,
                ),
            ),
        )

    fun emailRegister(request: EmailRegisterRequest): AuthTokenResponse =
        authApiMapper.toResponse(authService.emailRegister(EmailRegisterCommand(request.email, request.password, request.displayName)))

    fun emailLogin(request: EmailLoginRequest): AuthTokenResponse =
        authApiMapper.toResponse(authService.emailLogin(EmailLoginCommand(request.email, request.password)))
}
