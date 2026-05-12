package com.blockchain2026.team4.backend.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "wallet_login_nonces")
class WalletLoginNonceEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "wallet_address", nullable = false, length = 64)
    var walletAddress: String,

    @Column(name = "nonce", nullable = false, unique = true, length = 120)
    var nonce: String,

    @Column(name = "message", nullable = false, columnDefinition = "text")
    var message: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "consumed_at")
    var consumedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
