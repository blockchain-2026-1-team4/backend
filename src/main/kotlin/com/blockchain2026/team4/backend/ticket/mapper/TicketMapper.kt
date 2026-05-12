package com.blockchain2026.team4.backend.ticket.mapper

import com.blockchain2026.team4.backend.ticket.dto.TicketDto
import com.blockchain2026.team4.backend.ticket.entity.TicketEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface TicketMapper {
    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "owner.id", target = "ownerId")
    fun toDto(entity: TicketEntity): TicketDto

    fun toDtos(entities: List<TicketEntity>): List<TicketDto>
}
