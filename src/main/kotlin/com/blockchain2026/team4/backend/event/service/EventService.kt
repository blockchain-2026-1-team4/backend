package com.blockchain2026.team4.backend.event.service

import com.blockchain2026.team4.backend.blockchain.dto.ContractEventCommand
import com.blockchain2026.team4.backend.blockchain.gateway.TrustTicketGateway
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.event.dto.EventCreateCommand
import com.blockchain2026.team4.backend.event.dto.EventDto
import com.blockchain2026.team4.backend.event.dto.EventStatusCommand
import com.blockchain2026.team4.backend.event.dto.EventUpdateCommand
import com.blockchain2026.team4.backend.event.entity.EventEntity
import com.blockchain2026.team4.backend.event.entity.EventStatus
import com.blockchain2026.team4.backend.event.mapper.EventMapper
import com.blockchain2026.team4.backend.event.repository.EventRepository
import com.blockchain2026.team4.backend.user.entity.UserRole
import com.blockchain2026.team4.backend.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val userService: UserService,
    private val trustTicketGateway: TrustTicketGateway,
    private val blockchainTransactionService: BlockchainTransactionService,
    private val eventMapper: EventMapper,
) {
    @Transactional
    fun create(organizerId: UUID, command: EventCreateCommand): EventDto {
        validateSalesPolicy(command)
        val organizer = userService.requireRole(organizerId, UserRole.ORGANIZER)
        val organizerEntity = userService.findEntity(organizer.id)

        val submission = trustTicketGateway.createEvent(
            ContractEventCommand(
                eventName = command.name,
                eventTimestamp = command.eventAt.epochSecond.toBigInteger(),
                ticketPriceWei = command.ticketPriceWei,
                totalTicketCount = command.totalTicketCount.toBigInteger(),
                primarySaleStart = command.primarySaleStart.epochSecond.toBigInteger(),
                primarySaleEnd = command.primarySaleEnd.epochSecond.toBigInteger(),
                resaleAllowed = command.resaleAllowed,
                maxResalePriceRate = command.maxResalePriceRate.toBigInteger(),
                resaleStart = (command.resaleStart ?: command.primarySaleStart).epochSecond.toBigInteger(),
                resaleEnd = (command.resaleEnd ?: command.primarySaleEnd).epochSecond.toBigInteger(),
            ),
        )
        blockchainTransactionService.record(submission)

        val event = eventRepository.save(
            EventEntity(
                organizer = organizerEntity,
                name = command.name,
                description = command.description,
                category = command.category,
                venue = command.venue,
                imageUrl = command.imageUrl,
                eventAt = command.eventAt,
                ticketPriceWei = command.ticketPriceWei,
                totalTicketCount = command.totalTicketCount,
                primarySaleStart = command.primarySaleStart,
                primarySaleEnd = command.primarySaleEnd,
                resaleAllowed = command.resaleAllowed,
                maxResalePriceRate = command.maxResalePriceRate,
                resaleStart = command.resaleStart,
                resaleEnd = command.resaleEnd,
            ),
        )
        return eventMapper.toDto(event)
    }

    @Transactional(readOnly = true)
    fun get(eventId: UUID): EventDto = eventMapper.toDto(findEntity(eventId))

    @Transactional(readOnly = true)
    fun list(page: Int, size: Int, status: EventStatus?): PageResponse<EventDto> {
        val pageable = PageRequest.of(page, size)
        val events = status?.let { eventRepository.findAllByStatus(it, pageable) } ?: eventRepository.findAll(pageable)
        return PageResponse(
            items = events.content.map(eventMapper::toDto),
            page = events.number,
            size = events.size,
            totalElements = events.totalElements,
            totalPages = events.totalPages,
            hasNext = events.hasNext(),
        )
    }

    @Transactional(readOnly = true)
    fun listByOrganizer(organizerId: UUID, page: Int, size: Int): PageResponse<EventDto> {
        val events = eventRepository.findAllByOrganizerId(organizerId, PageRequest.of(page, size))
        return PageResponse(
            items = events.content.map(eventMapper::toDto),
            page = events.number,
            size = events.size,
            totalElements = events.totalElements,
            totalPages = events.totalPages,
            hasNext = events.hasNext(),
        )
    }

    @Transactional
    fun update(organizerId: UUID, eventId: UUID, command: EventUpdateCommand): EventDto {
        val event = findEntity(eventId)
        requireOrganizer(event, organizerId)
        command.name?.let { event.name = it }
        command.description?.let { event.description = it }
        command.category?.let { event.category = it }
        command.venue?.let { event.venue = it }
        command.imageUrl?.let { event.imageUrl = it }
        command.eventAt?.let { event.eventAt = it }
        return eventMapper.toDto(event)
    }

    @Transactional
    fun changeStatus(actorId: UUID, eventId: UUID, command: EventStatusCommand): EventDto {
        val event = findEntity(eventId)
        val actor = userService.getUser(actorId)
        if (actor.id != event.organizer.id && !actor.roles.contains(UserRole.ADMIN)) {
            throw BusinessException(ErrorCode.FORBIDDEN, "이벤트 상태 변경 권한이 없습니다.")
        }

        event.status = command.status
        val active = command.status == EventStatus.ACTIVE
        event.contractEventId?.let {
            val submission = trustTicketGateway.setEventStatus(it, active)
            blockchainTransactionService.record(submission)
        }
        return eventMapper.toDto(event)
    }

    @Transactional
    fun updateImage(organizerId: UUID, eventId: UUID, imageUrl: String): EventDto {
        val event = findEntity(eventId)
        requireOrganizer(event, organizerId)
        event.imageUrl = imageUrl
        return eventMapper.toDto(event)
    }

    @Transactional(readOnly = true)
    fun findEntity(eventId: UUID): EventEntity =
        eventRepository.findById(eventId)
            .orElseThrow { BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이벤트를 찾을 수 없습니다.") }

    fun countActive(): Long = eventRepository.countByStatus(EventStatus.ACTIVE)

    private fun requireOrganizer(event: EventEntity, organizerId: UUID) {
        if (event.organizer.id != organizerId) {
            throw BusinessException(ErrorCode.FORBIDDEN, "해당 이벤트의 주최자만 처리할 수 있습니다.")
        }
    }

    private fun validateSalesPolicy(command: EventCreateCommand) {
        if (command.ticketPriceWei <= BigInteger.ZERO) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "티켓 가격은 0보다 커야 합니다.")
        }
        if (command.totalTicketCount <= 0) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "티켓 수량은 1개 이상이어야 합니다.")
        }
        if (!command.primarySaleStart.isBefore(command.primarySaleEnd)) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "1차 판매 시작 시간은 종료 시간보다 빨라야 합니다.")
        }
        if (command.resaleAllowed) {
            val resaleStart = command.resaleStart ?: throw BusinessException(ErrorCode.INVALID_REQUEST, "리셀 시작 시간이 필요합니다.")
            val resaleEnd = command.resaleEnd ?: throw BusinessException(ErrorCode.INVALID_REQUEST, "리셀 종료 시간이 필요합니다.")
            if (!resaleStart.isBefore(resaleEnd)) {
                throw BusinessException(ErrorCode.INVALID_REQUEST, "리셀 시작 시간은 종료 시간보다 빨라야 합니다.")
            }
            if (command.maxResalePriceRate < 10_000) {
                throw BusinessException(ErrorCode.INVALID_REQUEST, "리셀 상한은 100% 이상이어야 합니다.")
            }
        }
        if (command.eventAt.isBefore(Instant.now().minusSeconds(60))) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "지난 이벤트는 등록할 수 없습니다.")
        }
    }
}
