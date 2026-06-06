package com.blockchain2026.team4.backend.event.repository

import com.blockchain2026.team4.backend.event.entity.EventRoundEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventRoundRepository : JpaRepository<EventRoundEntity, UUID> {
    fun findAllByEventIdOrderByEventDateAscStartTimeAsc(eventId: UUID): List<EventRoundEntity>
    fun deleteAllByEventId(eventId: UUID)
}
