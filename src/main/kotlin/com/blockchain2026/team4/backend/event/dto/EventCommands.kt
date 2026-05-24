package com.blockchain2026.team4.backend.event.dto

import com.blockchain2026.team4.backend.event.entity.EventStatus
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class EventCreateCommand(
    val name: String,
    val description: String?,
    val category: String,
    val venue: String,
    val venuePlaceId: String?,
    val imageUrl: String?,
    val eventAt: Instant,
    val eventStartAt: Instant,
    val eventEndAt: Instant,
    val ticketPriceWei: BigInteger,
    val totalTicketCount: Int,
    val primarySaleStart: Instant,
    val primarySaleEnd: Instant,
    val resaleAllowed: Boolean,
    val maxResalePriceRate: Int,
    val resaleStart: Instant?,
    val resaleEnd: Instant?,
    val rounds: List<EventRoundCommand>,
)

data class EventUpdateCommand(
    val name: String?,
    val description: String?,
    val category: String?,
    val venue: String?,
    val venuePlaceId: String?,
    val imageUrl: String?,
    val eventAt: Instant?,
    val eventStartAt: Instant?,
    val eventEndAt: Instant?,
    val primarySaleStart: Instant?,
    val primarySaleEnd: Instant?,
    val rounds: List<EventRoundCommand>?,
)

data class EventRoundCommand(
    val title: String,
    val eventDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val saleStartAt: Instant,
    val saleEndAt: Instant,
    val useGlobalSalePeriod: Boolean,
)

data class EventResalePolicyCommand(
    val resaleAllowed: Boolean,
    val maxResalePriceRate: Int,
    val resaleStart: Instant?,
    val resaleEnd: Instant?,
)

data class EventStatusCommand(
    val status: EventStatus,
)
