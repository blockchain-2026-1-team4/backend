package com.blockchain2026.team4.backend.organizer.mapper

import com.blockchain2026.team4.backend.organizer.dto.OrganizerApplicationDto
import com.blockchain2026.team4.backend.organizer.entity.OrganizerApplicationEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface OrganizerApplicationMapper {
    @Mapping(source = "user.id", target = "userId")
    fun toDto(entity: OrganizerApplicationEntity): OrganizerApplicationDto

    fun toDtos(entities: List<OrganizerApplicationEntity>): List<OrganizerApplicationDto>
}
