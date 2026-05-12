package com.blockchain2026.team4.backend.organizer.mapper

import com.blockchain2026.team4.backend.organizer.controller.response.OrganizerApplicationResponse
import com.blockchain2026.team4.backend.organizer.dto.OrganizerApplicationDto
import org.mapstruct.Mapper

@Mapper
interface OrganizerApplicationApiMapper {
    fun toResponse(dto: OrganizerApplicationDto): OrganizerApplicationResponse

    fun toResponses(dtos: List<OrganizerApplicationDto>): List<OrganizerApplicationResponse>
}
