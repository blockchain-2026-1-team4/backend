package com.blockchain2026.team4.backend.event.mapper

import com.blockchain2026.team4.backend.event.dto.EventDto
import com.blockchain2026.team4.backend.event.dto.EventRoundDto
import com.blockchain2026.team4.backend.event.entity.EventEntity
import com.blockchain2026.team4.backend.event.entity.EventRoundEntity
import org.springframework.stereotype.Component

@Component
class EventMapper {
    fun toDto(entity: EventEntity, rounds: List<EventRoundEntity> = emptyList()): EventDto =
        EventDto(
            id = entity.id,
            organizerId = entity.organizer.id,
            contractEventId = entity.contractEventId,
            name = entity.name,
            description = entity.description,
            category = entity.category,
            venue = entity.venue,
            venuePlaceId = entity.venuePlaceId,
            imageUrl = entity.imageUrl,
            eventAt = entity.eventAt,
            eventStartAt = entity.eventStartAt,
            eventEndAt = entity.eventEndAt,
            ticketPriceWei = entity.ticketPriceWei,
            totalTicketCount = entity.totalTicketCount,
            remainingTicketCount = entity.remainingTicketCount,
            soldTicketCount = entity.soldTicketCount,
            primarySaleStart = entity.primarySaleStart,
            primarySaleEnd = entity.primarySaleEnd,
            resaleAllowed = entity.resaleAllowed,
            maxResalePriceRate = entity.maxResalePriceRate,
            resaleStart = entity.resaleStart,
            resaleEnd = entity.resaleEnd,
            flagged = entity.flagged,
            adminCanceled = entity.adminCanceled,
            status = entity.status,
            rounds = rounds.map(::toRoundDto),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )

    private fun toRoundDto(entity: EventRoundEntity): EventRoundDto =
        EventRoundDto(
            id = entity.id,
            title = entity.title,
            eventDate = entity.eventDate,
            startTime = entity.startTime,
            endTime = entity.endTime,
            saleStartAt = entity.saleStartAt,
            saleEndAt = entity.saleEndAt,
            useGlobalSalePeriod = entity.useGlobalSalePeriod,
        )
}
