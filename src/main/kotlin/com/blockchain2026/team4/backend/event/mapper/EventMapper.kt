package com.blockchain2026.team4.backend.event.mapper

import com.blockchain2026.team4.backend.event.dto.EventDto
import com.blockchain2026.team4.backend.event.entity.EventEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface EventMapper {
    @Mapping(source = "organizer.id", target = "organizerId")
    fun toDto(entity: EventEntity): EventDto
}
