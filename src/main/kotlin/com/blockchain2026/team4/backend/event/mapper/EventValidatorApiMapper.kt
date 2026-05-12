package com.blockchain2026.team4.backend.event.mapper

import com.blockchain2026.team4.backend.event.controller.response.EventValidatorResponse
import com.blockchain2026.team4.backend.event.dto.EventValidatorDto
import org.mapstruct.Mapper

@Mapper
interface EventValidatorApiMapper {
    fun toResponse(dto: EventValidatorDto): EventValidatorResponse

    fun toResponses(dtos: List<EventValidatorDto>): List<EventValidatorResponse>
}
