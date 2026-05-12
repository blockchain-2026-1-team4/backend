package com.blockchain2026.team4.backend.ticket.entity

import com.blockchain2026.team4.backend.event.entity.EventEntity
import com.blockchain2026.team4.backend.user.entity.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tickets")
class TicketEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    var event: EventEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: UserEntity? = null,

    @Column(name = "contract_token_id", precision = 78, scale = 0)
    var contractTokenId: BigInteger? = null,

    @Column(name = "seat_info", nullable = false, length = 120)
    var seatInfo: String,

    @Column(name = "original_price_wei", nullable = false, precision = 78, scale = 0)
    var originalPriceWei: BigInteger,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: TicketStatus = TicketStatus.AVAILABLE,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @PrePersist
    fun prePersist() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}
