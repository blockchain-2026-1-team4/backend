package com.blockchain2026.team4.backend.checkin.mapper

import com.blockchain2026.team4.backend.checkin.dto.CheckInRecordDto
import com.blockchain2026.team4.backend.checkin.entity.CheckInRecordEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface CheckInRecordMapper {
    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "validator.id", target = "validatorId")
    fun toDto(entity: CheckInRecordEntity): CheckInRecordDto
}
