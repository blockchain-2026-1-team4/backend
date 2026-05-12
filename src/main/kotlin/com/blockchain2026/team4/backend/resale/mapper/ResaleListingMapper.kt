package com.blockchain2026.team4.backend.resale.mapper

import com.blockchain2026.team4.backend.resale.dto.ResaleListingDto
import com.blockchain2026.team4.backend.resale.entity.ResaleListingEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface ResaleListingMapper {
    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "ticket.event.id", target = "eventId")
    @Mapping(source = "seller.id", target = "sellerId")
    fun toDto(entity: ResaleListingEntity): ResaleListingDto
}
