package com.blockchain2026.team4.backend.checkin.entity

import com.blockchain2026.team4.backend.ticket.entity.TicketEntity
import com.blockchain2026.team4.backend.user.entity.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "check_in_records")
class CheckInRecordEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    var ticket: TicketEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validator_id", nullable = false)
    var validator: UserEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 30)
    var result: CheckInResult,

    @Column(name = "checked_in_at", nullable = false)
    var checkedInAt: Instant = Instant.now(),

    @Column(name = "memo", columnDefinition = "text")
    var memo: String? = null,
)
