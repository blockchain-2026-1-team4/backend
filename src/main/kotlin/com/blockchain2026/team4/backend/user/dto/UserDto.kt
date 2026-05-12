package com.blockchain2026.team4.backend.user.dto

import com.blockchain2026.team4.backend.user.entity.UserRole
import com.blockchain2026.team4.backend.user.entity.UserStatus
import java.time.Instant
import java.util.UUID

data class UserDto(
    val id: UUID,
    val walletAddress: String?,
    val email: String?,
    val displayName: String?,
    val status: UserStatus,
    val roles: Set<UserRole>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
