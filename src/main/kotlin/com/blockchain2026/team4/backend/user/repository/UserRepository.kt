package com.blockchain2026.team4.backend.user.repository

import com.blockchain2026.team4.backend.user.entity.UserEntity
import com.blockchain2026.team4.backend.user.entity.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByWalletAddressIgnoreCase(walletAddress: String): UserEntity?

    fun findByEmailIgnoreCase(email: String): UserEntity?

    fun existsByEmailIgnoreCase(email: String): Boolean

    fun findAllByStatus(status: UserStatus, pageable: Pageable): Page<UserEntity>
}
