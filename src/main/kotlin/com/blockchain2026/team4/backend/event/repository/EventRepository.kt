package com.blockchain2026.team4.backend.event.repository

import com.blockchain2026.team4.backend.event.entity.EventEntity
import com.blockchain2026.team4.backend.event.entity.EventStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface EventRepository : JpaRepository<EventEntity, UUID> {
    fun findAllByStatus(status: EventStatus, pageable: Pageable): Page<EventEntity>

    @Query(
        """
        select e from EventEntity e
        where (:status is null or e.status = :status)
          and (:flagged is null or e.flagged = :flagged)
          and (:category is null or lower(e.category) = lower(:category))
          and (
            :query is null
            or lower(e.name) like lower(concat('%', :query, '%'))
            or lower(coalesce(e.description, '')) like lower(concat('%', :query, '%'))
            or lower(e.venue) like lower(concat('%', :query, '%'))
          )
        """,
    )
    fun search(
        @Param("status") status: EventStatus?,
        @Param("category") category: String?,
        @Param("query") query: String?,
        @Param("flagged") flagged: Boolean?,
        pageable: Pageable,
    ): Page<EventEntity>

    fun findAllByOrganizerId(organizerId: UUID, pageable: Pageable): Page<EventEntity>

    fun countByStatus(status: EventStatus): Long
}
