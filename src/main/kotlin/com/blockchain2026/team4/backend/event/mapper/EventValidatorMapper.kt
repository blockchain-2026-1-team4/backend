package com.blockchain2026.team4.backend.event.mapper

import com.blockchain2026.team4.backend.event.dto.EventValidatorDto
import com.blockchain2026.team4.backend.event.entity.EventValidatorEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface EventValidatorMapper {
    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "validator.id", target = "validatorId")
    @Mapping(source = "validator.displayName", target = "validatorDisplayName")
    @Mapping(source = "validator.email", target = "validatorEmail")
    @Mapping(source = "validator.walletAddress", target = "validatorWalletAddress")
    fun toDto(entity: EventValidatorEntity): EventValidatorDto

    fun toDtos(entities: List<EventValidatorEntity>): List<EventValidatorDto>
}
