package com.blockchain2026.team4.backend.event.controller.request

import com.blockchain2026.team4.backend.event.entity.EventStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigInteger
import java.time.Instant

data class EventCreateRequest(
    @field:NotBlank
    @field:Size(max = 180)
    val name: String,

    val description: String?,

    @field:NotBlank
    @field:Size(max = 80)
    val category: String,

    @field:NotBlank
    @field:Size(max = 180)
    val venue: String,

    val imageUrl: String?,

    @field:NotNull
    val eventAt: Instant,

    @field:NotNull
    val ticketPriceWei: BigInteger,

    @field:Min(1)
    val totalTicketCount: Int,

    @field:NotNull
    val primarySaleStart: Instant,

    @field:NotNull
    val primarySaleEnd: Instant,

    val resaleAllowed: Boolean,

    @field:Min(10_000)
    val maxResalePriceRate: Int,

    val resaleStart: Instant?,

    val resaleEnd: Instant?,
)

data class EventUpdateRequest(
    @field:Size(max = 180)
    val name: String?,
    val description: String?,
    @field:Size(max = 80)
    val category: String?,
    @field:Size(max = 180)
    val venue: String?,
    val imageUrl: String?,
    val eventAt: Instant?,
)

data class EventStatusRequest(
    val status: EventStatus,
)

data class EventResalePolicyRequest(
    val resaleAllowed: Boolean,

    @field:Min(10_000)
    val maxResalePriceRate: Int,

    val resaleStart: Instant?,

    val resaleEnd: Instant?,
)

data class EventValidatorRequest(
    @field:NotNull
    val userId: java.util.UUID,
)
