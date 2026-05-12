package com.blockchain2026.team4.backend.auth.repository

import com.blockchain2026.team4.backend.auth.entity.WalletLoginNonceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WalletLoginNonceRepository : JpaRepository<WalletLoginNonceEntity, UUID> {
    fun findByNonce(nonce: String): WalletLoginNonceEntity?
}
