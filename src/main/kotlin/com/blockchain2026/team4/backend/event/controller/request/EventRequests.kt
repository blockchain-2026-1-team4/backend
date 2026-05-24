package com.blockchain2026.team4.backend.event.controller.request

import com.blockchain2026.team4.backend.event.entity.EventStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class EventCreateRequest(
    @field:NotBlank
    @field:Size(max = 180)
    val name: String,

    val description: String? = null,

    @field:NotBlank
    @field:Size(max = 80)
    val category: String,

    @field:NotBlank
    @field:Size(max = 180)
    val venue: String,

    val location: EventLocationRequest? = null,

    val venuePlaceId: String? = null,

    val imageUrl: String? = null,

    val eventAt: Instant? = null,

    val eventStartAt: Instant? = null,

    val eventEndAt: Instant? = null,

    val startsAt: Instant? = null,

    val endsAt: Instant? = null,

    val ticketPriceWei: BigInteger? = null,

    val totalTicketCount: Int? = null,

    val primarySaleStart: Instant? = null,

    val primarySaleEnd: Instant? = null,

    val salesStartAt: Instant? = null,

    val salesEndAt: Instant? = null,

    val resaleAllowed: Boolean = false,

    val maxResalePriceRate: Int? = null,

    val resaleStart: Instant? = null,

    val resaleEnd: Instant? = null,

    val rounds: List<EventRoundRequest> = emptyList(),
)

data class EventUpdateRequest(
    @field:Size(max = 180)
    val name: String? = null,
    val description: String? = null,
    @field:Size(max = 80)
    val category: String? = null,
    @field:Size(max = 180)
    val venue: String? = null,
    val location: EventLocationRequest? = null,
    val venuePlaceId: String? = null,
    val imageUrl: String? = null,
    val eventAt: Instant? = null,
    val eventStartAt: Instant? = null,
    val eventEndAt: Instant? = null,
    val startsAt: Instant? = null,
    val endsAt: Instant? = null,
)

data class EventLocationRequest(
    val name: String? = null,
    val address: String? = null,
    val placeId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class EventRoundRequest(
    @field:Size(max = 80)
    val title: String? = null,
    @field:NotNull
    val eventDate: LocalDate,
    @field:NotNull
    val startTime: LocalTime,
    @field:NotNull
    val endTime: LocalTime,
    val saleStartAt: Instant? = null,
    val saleEndAt: Instant? = null,
    val useGlobalSalePeriod: Boolean = true,
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
