package com.blockchain2026.team4.backend.auth.controller.response

import com.blockchain2026.team4.backend.user.controller.response.UserResponse
import java.time.Instant

data class WalletNonceResponse(
    val walletAddress: String,
    val nonce: String,
    val message: String,
    val expiresAt: Instant,
)

data class AuthTokenResponse(
    val tokenType: String,
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse,
    val isNewUser: Boolean = false,
)
