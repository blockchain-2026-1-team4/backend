package com.blockchain2026.team4.backend.user.controller.request

import jakarta.validation.constraints.Size

data class UpdateMeRequest(
    @field:Size(max = 120)
    val displayName: String?,
)
