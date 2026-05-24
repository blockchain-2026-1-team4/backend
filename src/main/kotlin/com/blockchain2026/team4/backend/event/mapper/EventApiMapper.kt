package com.blockchain2026.team4.backend.event.mapper

import com.blockchain2026.team4.backend.event.controller.response.EventResponse
import com.blockchain2026.team4.backend.event.controller.response.EventRoundResponse
import com.blockchain2026.team4.backend.event.dto.EventDto
import com.blockchain2026.team4.backend.event.dto.EventRoundDto
import org.mapstruct.Mapper

@Mapper
interface EventApiMapper {
    fun toResponse(dto: EventDto): EventResponse

    fun toResponses(dtos: List<EventDto>): List<EventResponse>

    fun toRoundResponse(dto: EventRoundDto): EventRoundResponse
}
