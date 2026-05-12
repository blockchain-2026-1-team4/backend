package com.blockchain2026.team4.backend.dispute.mapper

import com.blockchain2026.team4.backend.dispute.controller.response.DisputeResponse
import com.blockchain2026.team4.backend.dispute.dto.DisputeDto
import org.mapstruct.Mapper

@Mapper
interface DisputeApiMapper {
    fun toResponse(dto: DisputeDto): DisputeResponse

    fun toResponses(dtos: List<DisputeDto>): List<DisputeResponse>
}
