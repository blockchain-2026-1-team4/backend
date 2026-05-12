package com.blockchain2026.team4.backend.dispute.mapper

import com.blockchain2026.team4.backend.dispute.dto.DisputeDto
import com.blockchain2026.team4.backend.dispute.entity.DisputeEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface DisputeMapper {
    @Mapping(source = "reporter.id", target = "reporterId")
    @Mapping(source = "resaleListing.id", target = "resaleListingId")
    @Mapping(source = "ticket.id", target = "ticketId")
    fun toDto(entity: DisputeEntity): DisputeDto
}
