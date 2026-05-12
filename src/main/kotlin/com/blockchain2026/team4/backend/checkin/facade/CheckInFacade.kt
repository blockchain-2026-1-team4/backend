package com.blockchain2026.team4.backend.checkin.facade

import com.blockchain2026.team4.backend.checkin.controller.request.CheckInRequest
import com.blockchain2026.team4.backend.checkin.controller.request.QrCreateRequest
import com.blockchain2026.team4.backend.checkin.controller.response.CheckInRecordResponse
import com.blockchain2026.team4.backend.checkin.controller.response.QrCodeResponse
import com.blockchain2026.team4.backend.checkin.dto.CheckInCommand
import com.blockchain2026.team4.backend.checkin.dto.QrCreateCommand
import com.blockchain2026.team4.backend.checkin.mapper.CheckInApiMapper
import com.blockchain2026.team4.backend.checkin.service.CheckInService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CheckInFacade(
    private val checkInService: CheckInService,
    private val checkInApiMapper: CheckInApiMapper,
) {
    fun createQr(userId: UUID, ticketId: UUID, request: QrCreateRequest): QrCodeResponse =
        checkInApiMapper.toResponse(
            checkInService.createQr(
                userId,
                QrCreateCommand(ticketId, request.claimedOwner, request.expiresAt, request.signature),
            ),
        )

    fun checkIn(validatorId: UUID, request: CheckInRequest): CheckInRecordResponse =
        checkInApiMapper.toResponse(
            checkInService.checkIn(
                validatorId,
                CheckInCommand(request.ticketId, request.claimedOwner, request.expiresAt, request.signature, request.memo),
            ),
        )

    fun history(ticketId: UUID): List<CheckInRecordResponse> =
        checkInApiMapper.toResponses(checkInService.history(ticketId))
}
