package com.blockchain2026.team4.backend.common.security

import com.blockchain2026.team4.backend.user.entity.UserRole
import java.util.UUID

data class AuthPrincipal(
    val userId: UUID,
    val roles: Set<UserRole>,
    val walletAddress: String?,
    val email: String?,
)
