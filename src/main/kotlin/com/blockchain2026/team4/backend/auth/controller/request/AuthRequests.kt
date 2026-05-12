package com.blockchain2026.team4.backend.auth.controller.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class WalletNonceRequest(
    @field:NotBlank
    val walletAddress: String,
)

data class WalletLoginRequest(
    @field:NotBlank
    val walletAddress: String,

    @field:NotBlank
    val nonce: String,

    @field:NotBlank
    val signature: String,
)

data class EmailRegisterRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:Size(min = 8, max = 100)
    val password: String,

    @field:Size(max = 120)
    val displayName: String?,
)

data class EmailLoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String,
)
