package com.blockchain2026.team4.backend.ticket.mapper

import com.blockchain2026.team4.backend.ticket.controller.response.TicketResponse
import com.blockchain2026.team4.backend.ticket.dto.TicketDto
import org.mapstruct.Mapper

@Mapper
interface TicketApiMapper {
    fun toResponse(dto: TicketDto): TicketResponse

    fun toResponses(dtos: List<TicketDto>): List<TicketResponse>
}
