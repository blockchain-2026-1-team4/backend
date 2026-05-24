package com.blockchain2026.team4.backend.event.service

import com.blockchain2026.team4.backend.blockchain.dto.ContractEventCommand
import com.blockchain2026.team4.backend.blockchain.gateway.TrustTicketGateway
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.event.dto.EventCreateCommand
import com.blockchain2026.team4.backend.event.dto.EventDto
import com.blockchain2026.team4.backend.event.dto.EventResalePolicyCommand
import com.blockchain2026.team4.backend.event.dto.EventStatusCommand
import com.blockchain2026.team4.backend.event.dto.EventUpdateCommand
import com.blockchain2026.team4.backend.event.dto.EventValidatorDto
import com.blockchain2026.team4.backend.event.entity.EventEntity
import com.blockchain2026.team4.backend.event.entity.EventRoundEntity
import com.blockchain2026.team4.backend.event.entity.EventStatus
import com.blockchain2026.team4.backend.event.entity.EventValidatorEntity
import com.blockchain2026.team4.backend.event.mapper.EventMapper
import com.blockchain2026.team4.backend.event.mapper.EventValidatorMapper
import com.blockchain2026.team4.backend.event.repository.EventRepository
import com.blockchain2026.team4.backend.event.repository.EventRoundRepository
import com.blockchain2026.team4.backend.event.repository.EventValidatorRepository
import com.blockchain2026.team4.backend.user.entity.UserRole
import com.blockchain2026.team4.backend.user.service.UserService
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val eventRoundRepository: EventRoundRepository,
    private val eventValidatorRepository: EventValidatorRepository,
    private val userService: UserService,
    private val trustTicketGateway: TrustTicketGateway,
    private val blockchainTransactionService: BlockchainTransactionService,
    private val eventMapper: EventMapper,
    private val eventValidatorMapper: EventValidatorMapper,
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
                totalTicketCount = command.totalTicketCount.coerceAtLeast(1).toBigInteger(),
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
                venuePlaceId = command.venuePlaceId,
                imageUrl = command.imageUrl,
                eventAt = command.eventAt,
                eventStartAt = command.eventStartAt,
                eventEndAt = command.eventEndAt,
                ticketPriceWei = command.ticketPriceWei,
                totalTicketCount = command.totalTicketCount,
                remainingTicketCount = command.totalTicketCount,
                soldTicketCount = 0,
                primarySaleStart = command.primarySaleStart,
                primarySaleEnd = command.primarySaleEnd,
                resaleAllowed = command.resaleAllowed,
                maxResalePriceRate = command.maxResalePriceRate,
                resaleStart = command.resaleStart,
                resaleEnd = command.resaleEnd,
            ),
        )
        val rounds = command.rounds.map {
            eventRoundRepository.save(
                EventRoundEntity(
                    event = event,
                    title = it.title,
                    eventDate = it.eventDate,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    saleStartAt = it.saleStartAt,
                    saleEndAt = it.saleEndAt,
                    useGlobalSalePeriod = it.useGlobalSalePeriod,
                ),
            )
        }
        return eventMapper.toDto(event, rounds)
    }

    @Transactional(readOnly = true)
    fun get(eventId: UUID): EventDto {
        val event = findEntity(eventId)
        return eventMapper.toDto(event, eventRoundRepository.findAllByEventIdOrderByEventDateAscStartTimeAsc(eventId))
    }

    @Transactional(readOnly = true)
    fun list(page: Int, size: Int, status: EventStatus?, category: String?, query: String?, flagged: Boolean? = null): PageResponse<EventDto> {
        val pageable = PageRequest.of(page, size)
        val events = eventRepository.findAll(eventSearchSpec(status, category, query, flagged), pageable)
        return PageResponse(
            items = events.content.map { eventMapper.toDto(it, eventRoundRepository.findAllByEventIdOrderByEventDateAscStartTimeAsc(it.id)) },
            page = events.number,
            size = events.size,
            totalElements = events.totalElements,
            totalPages = events.totalPages,
            hasNext = events.hasNext(),
        )
    }

    private fun eventSearchSpec(
        status: EventStatus?,
        category: String?,
        query: String?,
        flagged: Boolean?,
    ): Specification<EventEntity> =
        Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            status?.let {
                predicates += criteriaBuilder.equal(root.get<EventStatus>("status"), it)
            }

            flagged?.let {
                predicates += criteriaBuilder.equal(root.get<Boolean>("flagged"), it)
            }

            category?.trim()?.takeIf { it.isNotBlank() }?.let {
                predicates += criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("category")),
                    it.lowercase(),
                )
            }

            query?.trim()?.takeIf { it.isNotBlank() }?.let {
                val pattern = "%${it.lowercase()}%"
                predicates += criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("venue")), pattern),
                )
            }

            if (predicates.isEmpty()) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.and(*predicates.toTypedArray())
            }
        }

    @Transactional(readOnly = true)
    fun listByOrganizer(organizerId: UUID, page: Int, size: Int): PageResponse<EventDto> {
        val events = eventRepository.findAllByOrganizerId(organizerId, PageRequest.of(page, size))
        return PageResponse(
            items = events.content.map { eventMapper.toDto(it, eventRoundRepository.findAllByEventIdOrderByEventDateAscStartTimeAsc(it.id)) },
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
        command.venuePlaceId?.let { event.venuePlaceId = it }
        command.imageUrl?.let { event.imageUrl = it }
        command.eventAt?.let {
            event.eventAt = it
            event.eventStartAt = it
        }
        command.eventStartAt?.let {
            event.eventAt = it
            event.eventStartAt = it
        }
        command.eventEndAt?.let { event.eventEndAt = it }
        command.primarySaleStart?.let { event.primarySaleStart = it }
        command.primarySaleEnd?.let { event.primarySaleEnd = it }
        if (event.primarySaleEnd <= event.primarySaleStart) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "티켓 판매 종료일은 판매 시작일보다 늦어야 합니다.")
        }
        command.rounds?.let { rounds ->
            validateRounds(rounds)
            eventRoundRepository.deleteAllByEventId(eventId)
            rounds.forEach {
                eventRoundRepository.save(
                    EventRoundEntity(
                        event = event,
                        title = it.title,
                        eventDate = it.eventDate,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        saleStartAt = it.saleStartAt,
                        saleEndAt = it.saleEndAt,
                        useGlobalSalePeriod = it.useGlobalSalePeriod,
                    ),
                )
            }
            val first = rounds.minBy { it.eventDate.atTime(it.startTime) }
            val last = rounds.maxBy { it.eventDate.atTime(it.endTime) }
            event.eventAt = first.eventDate.atTime(first.startTime).atZone(ZoneId.systemDefault()).toInstant()
            event.eventStartAt = event.eventAt
            event.eventEndAt = last.eventDate.atTime(last.endTime).atZone(ZoneId.systemDefault()).toInstant()
        }
        return eventMapper.toDto(event, eventRoundRepository.findAllByEventIdOrderByEventDateAscStartTimeAsc(eventId))
    }

    @Transactional
    fun updateResalePolicy(organizerId: UUID, eventId: UUID, command: EventResalePolicyCommand): EventDto {
        val event = findEntity(eventId)
        requireOrganizer(event, organizerId)
        validateResalePolicy(
            resaleAllowed = command.resaleAllowed,
            maxResalePriceRate = command.maxResalePriceRate,
            resaleStart = command.resaleStart,
            resaleEnd = command.resaleEnd,
        )
        event.resaleAllowed = command.resaleAllowed
        event.maxResalePriceRate = command.maxResalePriceRate
        event.resaleStart = command.resaleStart
        event.resaleEnd = command.resaleEnd
        return eventMapper.toDto(event, eventRoundRepository.findAllByEventIdOrderByEventDateAscStartTimeAsc(eventId))
    }

    @Transactional
    fun changeStatus(actorId: UUID, eventId: UUID, command: EventStatusCommand): EventDto {
        val event = findEntity(eventId)
        val actor = userService.getUser(actorId)
        val isAdmin = actor.roles.contains(UserRole.ADMIN)
        if (actor.id != event.organizer.id && !isAdmin) {
            throw BusinessException(ErrorCode.FORBIDDEN, "이벤트 상태 변경 권한이 없습니다.")
        }
        if (!isAdmin && event.adminCanceled && command.status != EventStatus.CANCELED) {
            throw BusinessException(ErrorCode.FORBIDDEN, "관리자가 취소한 이벤트는 주최자가 복구할 수 없습니다.")
        }

        event.status = command.status
        if (command.status == EventStatus.CANCELED) {
            event.adminCanceled = event.adminCanceled || isAdmin
        } else if (isAdmin) {
            event.adminCanceled = false
        }
        val active = command.status == EventStatus.ACTIVE
        event.contractEventId?.let {
            val submission = trustTicketGateway.setEventStatus(it, active)
            blockchainTransactionService.record(submission)
        }
        return eventMapper.toDto(event, eventRoundRepository.findAllByEventIdOrderByEventDateAscStartTimeAsc(eventId))
    }

    @Transactional
    fun flag(eventId: UUID, flagged: Boolean): EventDto {
        val event = findEntity(eventId)
        event.flagged = flagged
        return eventMapper.toDto(event, eventRoundRepository.findAllByEventIdOrderByEventDateAscStartTimeAsc(eventId))
    }

    @Transactional
    fun updateImage(organizerId: UUID, eventId: UUID, imageUrl: String): EventDto {
        val event = findEntity(eventId)
        requireOrganizer(event, organizerId)
        event.imageUrl = imageUrl
        return eventMapper.toDto(event, eventRoundRepository.findAllByEventIdOrderByEventDateAscStartTimeAsc(eventId))
    }

    @Transactional(readOnly = true)
    fun findEntity(eventId: UUID): EventEntity =
        eventRepository.findById(eventId)
            .orElseThrow { BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이벤트를 찾을 수 없습니다.") }

    fun countActive(): Long = eventRepository.countByStatus(EventStatus.ACTIVE)

    @Transactional
    fun registerPrimarySale(event: EventEntity) {
        if (event.remainingTicketCount <= 0) {
            throw BusinessException(ErrorCode.CONFLICT, "남은 티켓 수량이 없습니다.")
        }
        event.remainingTicketCount -= 1
        event.soldTicketCount += 1
    }

    @Transactional
    fun addValidator(actorId: UUID, eventId: UUID, validatorId: UUID): EventValidatorDto {
        val event = findEntity(eventId)
        val actor = userService.getUser(actorId)
        if (actor.id != event.organizer.id && !actor.roles.contains(UserRole.ADMIN)) {
            throw BusinessException(ErrorCode.FORBIDDEN, "이벤트 검증자 등록 권한이 없습니다.")
        }
        val validator = userService.findEntity(validatorId)
        val existing = eventValidatorRepository.findByEventIdAndValidatorId(eventId, validatorId)
        val saved = existing ?: eventValidatorRepository.save(EventValidatorEntity(event = event, validator = validator))

        if (existing == null) {
            event.contractEventId?.let { contractEventId ->
                validator.walletAddress?.let { wallet ->
                    val submission = trustTicketGateway.addEventValidator(contractEventId, wallet)
                    blockchainTransactionService.record(submission)
                }
            }
        }
        return eventValidatorMapper.toDto(saved)
    }

    @Transactional(readOnly = true)
    fun listValidators(eventId: UUID): List<EventValidatorDto> =
        eventValidatorMapper.toDtos(eventValidatorRepository.findAllByEventId(eventId))

    @Transactional(readOnly = true)
    fun canValidate(eventId: UUID, userId: UUID): Boolean {
        val user = userService.getUser(userId)
        return user.roles.any { it == UserRole.ADMIN || it == UserRole.VALIDATOR } ||
            eventValidatorRepository.existsByEventIdAndValidatorId(eventId, userId)
    }

    private fun requireOrganizer(event: EventEntity, organizerId: UUID) {
        if (event.organizer.id != organizerId) {
            throw BusinessException(ErrorCode.FORBIDDEN, "해당 이벤트의 주최자만 처리할 수 있습니다.")
        }
    }

    private fun validateSalesPolicy(command: EventCreateCommand) {
        if (command.ticketPriceWei <= BigInteger.ZERO) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "티켓 가격은 0보다 커야 합니다.")
        }
        if (command.totalTicketCount < 0) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "티켓 수량은 0개 이상이어야 합니다.")
        }
        if (command.eventEndAt.isBefore(command.eventStartAt)) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "이벤트 종료 시간은 시작 시간보다 빠를 수 없습니다.")
        }
        validateRounds(command.rounds)
        if (!command.primarySaleStart.isBefore(command.primarySaleEnd)) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "1차 판매 시작 시간은 종료 시간보다 빨라야 합니다.")
        }
        if (command.resaleAllowed) {
            validateResalePolicy(command.resaleAllowed, command.maxResalePriceRate, command.resaleStart, command.resaleEnd)
        }
        if (command.eventAt.isBefore(Instant.now().minusSeconds(60))) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "지난 이벤트는 등록할 수 없습니다.")
        }
    }

    private fun validateRounds(rounds: List<com.blockchain2026.team4.backend.event.dto.EventRoundCommand>) {
        if (rounds.isEmpty()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "최소 1개 회차가 필요합니다.")
        }
        val zoneId = ZoneId.systemDefault()
        val ranges = rounds.mapIndexed { index, round ->
            if (!round.startTime.isBefore(round.endTime)) {
                throw BusinessException(ErrorCode.INVALID_REQUEST, "${index + 1}회차 종료 시간은 시작 시간보다 늦어야 합니다.")
            }
            val startAt = round.eventDate.atTime(round.startTime).atZone(zoneId).toInstant()
            val endAt = round.eventDate.atTime(round.endTime).atZone(zoneId).toInstant()
            if (round.saleEndAt.isAfter(startAt)) {
                throw BusinessException(ErrorCode.INVALID_REQUEST, "${index + 1}회차 판매 종료일은 공연 시작 이후일 수 없습니다.")
            }
            if (!round.saleStartAt.isBefore(round.saleEndAt)) {
                throw BusinessException(ErrorCode.INVALID_REQUEST, "${index + 1}회차 판매 종료일은 판매 시작일보다 늦어야 합니다.")
            }
            startAt to endAt
        }.sortedBy { it.first }

        ranges.zipWithNext().forEachIndexed { index, (current, next) ->
            if (current.second.isAfter(next.first)) {
                throw BusinessException(ErrorCode.INVALID_REQUEST, "${index + 1}회차와 ${index + 2}회차 시간이 겹칩니다.")
            }
        }
    }

    private fun validateResalePolicy(
        resaleAllowed: Boolean,
        maxResalePriceRate: Int,
        resaleStart: Instant?,
        resaleEnd: Instant?,
    ) {
        if (!resaleAllowed) return
        val start = resaleStart ?: throw BusinessException(ErrorCode.INVALID_REQUEST, "리셀 시작 시간이 필요합니다.")
        val end = resaleEnd ?: throw BusinessException(ErrorCode.INVALID_REQUEST, "리셀 종료 시간이 필요합니다.")
        if (!start.isBefore(end)) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "리셀 시작 시간은 종료 시간보다 빨라야 합니다.")
        }
        if (maxResalePriceRate < 10_000) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "리셀 상한은 100% 이상이어야 합니다.")
        }
    }
}
