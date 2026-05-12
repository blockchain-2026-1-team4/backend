package com.blockchain2026.team4.backend.auth.dto

import java.time.Instant

data class WalletNonceDto(
    val walletAddress: String,
    val nonce: String,
    val message: String,
    val expiresAt: Instant,
)
