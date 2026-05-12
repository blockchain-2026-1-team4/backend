package com.blockchain2026.team4.backend.event.repository

import com.blockchain2026.team4.backend.event.entity.EventValidatorEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventValidatorRepository : JpaRepository<EventValidatorEntity, UUID> {
    fun existsByEventIdAndValidatorId(eventId: UUID, validatorId: UUID): Boolean

    fun findByEventIdAndValidatorId(eventId: UUID, validatorId: UUID): EventValidatorEntity?

    fun findAllByEventId(eventId: UUID): List<EventValidatorEntity>
}
