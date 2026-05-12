package com.blockchain2026.team4.backend.auth.dto

import com.blockchain2026.team4.backend.user.dto.UserDto

data class AuthTokensDto(
    val tokenType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto,
)
