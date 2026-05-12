package com.blockchain2026.team4.backend.event.repository

import com.blockchain2026.team4.backend.event.entity.EventEntity
import com.blockchain2026.team4.backend.event.entity.EventStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventRepository : JpaRepository<EventEntity, UUID> {
    fun findAllByStatus(status: EventStatus, pageable: Pageable): Page<EventEntity>

    fun findAllByOrganizerId(organizerId: UUID, pageable: Pageable): Page<EventEntity>

    fun countByStatus(status: EventStatus): Long
}
