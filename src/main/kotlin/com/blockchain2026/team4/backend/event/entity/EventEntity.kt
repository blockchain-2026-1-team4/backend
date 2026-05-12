package com.blockchain2026.team4.backend.event.entity

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
@Table(name = "events")
class EventEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    var organizer: UserEntity,

    @Column(name = "contract_event_id", precision = 78, scale = 0)
    var contractEventId: BigInteger? = null,

    @Column(name = "name", nullable = false, length = 180)
    var name: String,

    @Column(name = "description", columnDefinition = "text")
    var description: String?,

    @Column(name = "category", nullable = false, length = 80)
    var category: String,

    @Column(name = "venue", nullable = false, length = 180)
    var venue: String,

    @Column(name = "image_url", length = 500)
    var imageUrl: String?,

    @Column(name = "event_at", nullable = false)
    var eventAt: Instant,

    @Column(name = "ticket_price_wei", nullable = false, precision = 78, scale = 0)
    var ticketPriceWei: BigInteger,

    @Column(name = "total_ticket_count", nullable = false)
    var totalTicketCount: Int,

    @Column(name = "primary_sale_start", nullable = false)
    var primarySaleStart: Instant,

    @Column(name = "primary_sale_end", nullable = false)
    var primarySaleEnd: Instant,

    @Column(name = "resale_allowed", nullable = false)
    var resaleAllowed: Boolean,

    @Column(name = "max_resale_price_rate", nullable = false)
    var maxResalePriceRate: Int,

    @Column(name = "resale_start")
    var resaleStart: Instant?,

    @Column(name = "resale_end")
    var resaleEnd: Instant?,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: EventStatus = EventStatus.ACTIVE,

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
