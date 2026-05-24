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

    val eventAt: Instant?,

    val eventStartAt: Instant?,

    val eventEndAt: Instant?,

    val startsAt: Instant?,

    val endsAt: Instant?,

    val ticketPriceWei: BigInteger? = null,

    val totalTicketCount: Int? = null,

    val primarySaleStart: Instant?,

    val primarySaleEnd: Instant?,

    val salesStartAt: Instant?,

    val salesEndAt: Instant?,

    val resaleAllowed: Boolean = false,

    val maxResalePriceRate: Int? = null,

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
    val eventStartAt: Instant?,
    val eventEndAt: Instant?,
    val startsAt: Instant?,
    val endsAt: Instant?,
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
