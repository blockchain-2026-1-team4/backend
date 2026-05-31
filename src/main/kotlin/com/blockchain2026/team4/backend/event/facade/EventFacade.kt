package com.blockchain2026.team4.backend.event.facade

import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.storage.LocalImageStorageService
import com.blockchain2026.team4.backend.event.controller.request.EventCreateRequest
import com.blockchain2026.team4.backend.event.controller.request.EventResalePolicyRequest
import com.blockchain2026.team4.backend.event.controller.request.EventStatusRequest
import com.blockchain2026.team4.backend.event.controller.request.EventUpdateRequest
import com.blockchain2026.team4.backend.event.controller.request.EventValidatorRequest
import com.blockchain2026.team4.backend.event.controller.response.EventResponse
import com.blockchain2026.team4.backend.event.controller.response.EventValidatorResponse
import com.blockchain2026.team4.backend.event.dto.EventCreateCommand
import com.blockchain2026.team4.backend.event.dto.EventResalePolicyCommand
import com.blockchain2026.team4.backend.event.dto.EventRoundCommand
import com.blockchain2026.team4.backend.event.dto.EventStatusCommand
import com.blockchain2026.team4.backend.event.dto.EventUpdateCommand
import com.blockchain2026.team4.backend.event.entity.EventStatus
import com.blockchain2026.team4.backend.event.mapper.EventApiMapper
import com.blockchain2026.team4.backend.event.mapper.EventValidatorApiMapper
import com.blockchain2026.team4.backend.event.service.EventService
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

@Component
class EventFacade(
    private val eventService: EventService,
    private val eventApiMapper: EventApiMapper,
    private val eventValidatorApiMapper: EventValidatorApiMapper,
    private val localImageStorageService: LocalImageStorageService,
) {
    fun create(organizerId: UUID, request: EventCreateRequest): EventResponse {
        val start = request.eventStartAt ?: request.startsAt ?: request.eventAt ?: Instant.now()
        val end = request.eventEndAt ?: request.endsAt ?: request.eventAt?.plusSeconds(2 * 60 * 60) ?: start
        val saleStart = request.primarySaleStart ?: request.salesStartAt ?: Instant.now()
        val saleEnd = request.primarySaleEnd ?: request.salesEndAt ?: end
        val rounds = if (request.rounds.isNotEmpty()) {
            request.rounds.mapIndexed { index, round ->
                val roundStartInstant = round.eventDate.atTime(round.startTime).atZone(java.time.ZoneId.systemDefault()).toInstant()
                EventRoundCommand(
                    title = round.title?.takeIf { it.isNotBlank() } ?: "${index + 1}회차",
                    eventDate = round.eventDate,
                    startTime = round.startTime,
                    endTime = round.endTime,
                    saleStartAt = if (round.useGlobalSalePeriod) saleStart else round.saleStartAt ?: saleStart,
                    saleEndAt = if (round.useGlobalSalePeriod) saleEnd else round.saleEndAt ?: roundStartInstant,
                    useGlobalSalePeriod = round.useGlobalSalePeriod,
                )
            }
        } else {
            val roundStart = start.atZone(java.time.ZoneId.systemDefault())
            val roundEnd = end.atZone(java.time.ZoneId.systemDefault())
            listOf(
                EventRoundCommand(
                    title = "1회차",
                    eventDate = roundStart.toLocalDate(),
                    startTime = roundStart.toLocalTime(),
                    endTime = if (roundEnd.toLocalDate() == roundStart.toLocalDate()) roundEnd.toLocalTime() else java.time.LocalTime.of(23, 59),
                    saleStartAt = saleStart,
                    saleEndAt = saleEnd,
                    useGlobalSalePeriod = true,
                ),
            )
        }
        return eventApiMapper.toResponse(
            eventService.create(
                organizerId,
                EventCreateCommand(
                    name = request.name,
                    description = request.description,
                    category = request.category,
                    venue = request.location?.name?.takeIf { it.isNotBlank() } ?: request.venue,
                    venuePlaceId = request.location?.placeId ?: request.venuePlaceId,
                    imageUrl = request.imageUrl,
                    eventAt = start,
                    eventStartAt = start,
                    eventEndAt = end,
                    ticketPriceWei = request.ticketPriceWei ?: BigInteger.ONE,
                    totalTicketCount = request.totalTicketCount ?: 0,
                    primarySaleStart = saleStart,
                    primarySaleEnd = saleEnd,
                    resaleAllowed = request.resaleAllowed,
                    maxResalePriceRate = request.maxResalePriceRate ?: 10_000,
                    resaleStart = request.resaleStart,
                    resaleEnd = request.resaleEnd,
                    rounds = rounds,
                ),
            ),
        )
    }

    fun get(eventId: UUID): EventResponse = eventApiMapper.toResponse(eventService.get(eventId))

    fun list(page: Int, size: Int, status: EventStatus?, category: String?, query: String?, flagged: Boolean? = null): PageResponse<EventResponse> {
        val events = eventService.list(page, size, status, category, query, flagged)
        return PageResponse(
            items = eventApiMapper.toResponses(events.items),
            page = events.page,
            size = events.size,
            totalElements = events.totalElements,
            totalPages = events.totalPages,
            hasNext = events.hasNext,
        )
    }

    fun listMine(organizerId: UUID, page: Int, size: Int): PageResponse<EventResponse> {
        val events = eventService.listByOrganizer(organizerId, page, size)
        return PageResponse(
            items = eventApiMapper.toResponses(events.items),
            page = events.page,
            size = events.size,
            totalElements = events.totalElements,
            totalPages = events.totalPages,
            hasNext = events.hasNext,
        )
    }

    fun update(organizerId: UUID, eventId: UUID, request: EventUpdateRequest): EventResponse {
        val saleStart = request.primarySaleStart ?: request.salesStartAt
        val saleEnd = request.primarySaleEnd ?: request.salesEndAt
        val rounds = request.rounds?.mapIndexed { index, round ->
            val roundStartInstant = round.eventDate.atTime(round.startTime).atZone(java.time.ZoneId.systemDefault()).toInstant()
            EventRoundCommand(
                title = round.title?.takeIf { it.isNotBlank() } ?: "${index + 1}회차",
                eventDate = round.eventDate,
                startTime = round.startTime,
                endTime = round.endTime,
                saleStartAt = if (round.useGlobalSalePeriod) saleStart ?: round.saleStartAt ?: Instant.now() else round.saleStartAt ?: saleStart ?: Instant.now(),
                saleEndAt = if (round.useGlobalSalePeriod) saleEnd ?: round.saleEndAt ?: roundStartInstant else round.saleEndAt ?: saleEnd ?: roundStartInstant,
                useGlobalSalePeriod = round.useGlobalSalePeriod,
            )
        }
        return eventApiMapper.toResponse(
            eventService.update(
                organizerId,
                eventId,
                EventUpdateCommand(
                    request.name,
                    request.description,
                    request.category,
                    request.location?.name?.takeIf { it.isNotBlank() } ?: request.venue,
                    request.location?.placeId ?: request.venuePlaceId,
                    request.imageUrl,
                    request.removeImage,
                    request.eventAt,
                    request.eventStartAt ?: request.startsAt,
                    request.eventEndAt ?: request.endsAt,
                    saleStart,
                    saleEnd,
                    rounds,
                ),
            ),
        )
    }

    fun changeStatus(actorId: UUID, eventId: UUID, request: EventStatusRequest): EventResponse =
        eventApiMapper.toResponse(eventService.changeStatus(actorId, eventId, EventStatusCommand(request.status)))

    fun updateResalePolicy(organizerId: UUID, eventId: UUID, request: EventResalePolicyRequest): EventResponse =
        eventApiMapper.toResponse(
            eventService.updateResalePolicy(
                organizerId,
                eventId,
                EventResalePolicyCommand(request.resaleAllowed, request.maxResalePriceRate, request.resaleStart, request.resaleEnd),
            ),
        )

    fun flag(eventId: UUID, flagged: Boolean): EventResponse =
        eventApiMapper.toResponse(eventService.flag(eventId, flagged))

    fun addValidator(actorId: UUID, eventId: UUID, request: EventValidatorRequest): EventValidatorResponse =
        eventValidatorApiMapper.toResponse(eventService.addValidator(actorId, eventId, request.userId))

    fun listValidators(eventId: UUID): List<EventValidatorResponse> =
        eventValidatorApiMapper.toResponses(eventService.listValidators(eventId))

    fun uploadImage(organizerId: UUID, eventId: UUID, file: MultipartFile): EventResponse {
        val imageUrl = localImageStorageService.store(file)
        return eventApiMapper.toResponse(eventService.updateImage(organizerId, eventId, imageUrl))
    }
}
