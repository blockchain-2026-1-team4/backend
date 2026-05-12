package com.blockchain2026.team4.backend.checkin.mapper

import com.blockchain2026.team4.backend.checkin.controller.response.CheckInRecordResponse
import com.blockchain2026.team4.backend.checkin.controller.response.CheckInMessageResponse
import com.blockchain2026.team4.backend.checkin.controller.response.QrCodeResponse
import com.blockchain2026.team4.backend.checkin.dto.CheckInMessageDto
import com.blockchain2026.team4.backend.checkin.dto.CheckInRecordDto
import com.blockchain2026.team4.backend.checkin.dto.QrCodeDto
import org.mapstruct.Mapper

@Mapper
interface CheckInApiMapper {
    fun toResponse(dto: QrCodeDto): QrCodeResponse

    fun toResponse(dto: CheckInMessageDto): CheckInMessageResponse

    fun toResponse(dto: CheckInRecordDto): CheckInRecordResponse

    fun toResponses(dtos: List<CheckInRecordDto>): List<CheckInRecordResponse>
}
