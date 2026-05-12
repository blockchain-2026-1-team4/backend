package com.blockchain2026.team4.backend.resale.mapper

import com.blockchain2026.team4.backend.resale.controller.response.ResaleListingResponse
import com.blockchain2026.team4.backend.resale.dto.ResaleListingDto
import org.mapstruct.Mapper

@Mapper
interface ResaleApiMapper {
    fun toResponse(dto: ResaleListingDto): ResaleListingResponse

    fun toResponses(dtos: List<ResaleListingDto>): List<ResaleListingResponse>
}
