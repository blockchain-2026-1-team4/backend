package com.blockchain2026.team4.backend.auth.mapper

import com.blockchain2026.team4.backend.auth.controller.response.AuthTokenResponse
import com.blockchain2026.team4.backend.auth.controller.response.WalletNonceResponse
import com.blockchain2026.team4.backend.auth.dto.AuthTokensDto
import com.blockchain2026.team4.backend.auth.dto.WalletNonceDto
import com.blockchain2026.team4.backend.user.mapper.UserApiMapper
import org.springframework.stereotype.Component

@Component
class AuthApiMapper(
    private val userApiMapper: UserApiMapper,
) {
    fun toResponse(dto: WalletNonceDto): WalletNonceResponse =
        WalletNonceResponse(dto.walletAddress, dto.nonce, dto.message, dto.expiresAt)

    fun toResponse(dto: AuthTokensDto): AuthTokenResponse =
        AuthTokenResponse(
            tokenType = dto.tokenType,
            accessToken = dto.accessToken,
            refreshToken = dto.refreshToken,
            user = userApiMapper.toResponse(dto.user),
        )
}
