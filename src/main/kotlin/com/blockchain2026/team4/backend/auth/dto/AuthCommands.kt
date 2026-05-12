package com.blockchain2026.team4.backend.auth.dto

data class EmailRegisterCommand(
    val email: String,
    val password: String,
    val displayName: String?,
)

data class EmailLoginCommand(
    val email: String,
    val password: String,
)

data class WalletLoginCommand(
    val walletAddress: String,
    val nonce: String,
    val signature: String,
)
