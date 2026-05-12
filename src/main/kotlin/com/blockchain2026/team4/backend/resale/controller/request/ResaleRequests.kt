package com.blockchain2026.team4.backend.resale.controller.request

import jakarta.validation.constraints.NotNull
import java.math.BigInteger

data class ResaleCreateRequest(
    @field:NotNull
    val priceWei: BigInteger,
)
