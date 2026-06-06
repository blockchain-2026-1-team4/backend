package com.blockchain2026.team4.backend.resale.dto

import java.math.BigInteger

data class ResaleCreateCommand(
    val priceWei: BigInteger,
    val transactionHash: String? = null,
)

data class ResalePurchaseCommand(
    val transactionHash: String? = null,
)

data class ResaleCancelCommand(
    val transactionHash: String? = null,
)
