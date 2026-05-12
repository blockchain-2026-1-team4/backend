package com.blockchain2026.team4.backend.ticket.mapper

import com.blockchain2026.team4.backend.ticket.controller.response.TicketResponse
import com.blockchain2026.team4.backend.ticket.controller.response.TicketValidityResponse
import com.blockchain2026.team4.backend.ticket.dto.TicketDto
import com.blockchain2026.team4.backend.ticket.dto.TicketValidityDto
import org.mapstruct.Mapper

@Mapper
interface TicketApiMapper {
    fun toResponse(dto: TicketDto): TicketResponse

    fun toResponse(dto: TicketValidityDto): TicketValidityResponse

    fun toResponses(dtos: List<TicketDto>): List<TicketResponse>
}
