package com.blockchain2026.team4.backend.common.security

import com.blockchain2026.team4.backend.common.config.AppProperties
import com.blockchain2026.team4.backend.user.entity.UserEntity
import com.blockchain2026.team4.backend.user.entity.UserRole
import com.blockchain2026.team4.backend.user.entity.UserStatus
import com.blockchain2026.team4.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Service
class DevAuthenticationService(
    private val appProperties: AppProperties,
    private val userRepository: UserRepository,
) {
    private val superRoles = setOf(
        UserRole.USER,
        UserRole.ORGANIZER,
        UserRole.ADMIN,
        UserRole.VALIDATOR,
    )

    @Transactional
    fun authenticate(token: String): AuthPrincipal? {
        if (!isEnabled() || !tokenMatches(token)) return null
        val user = ensureDevUser()
        return AuthPrincipal(
            userId = user.id,
            roles = user.roles,
            walletAddress = user.walletAddress,
            email = user.email,
        )
    }

    @Transactional
    fun ensureDevUserIfEnabled() {
        if (isEnabled()) ensureDevUser()
    }

    private fun ensureDevUser(): UserEntity {
        val devAuth = appProperties.devAuth
        val user = userRepository.findById(devAuth.userId)
            .orElseGet { UserEntity(id = devAuth.userId) }

        user.walletAddress = devAuth.walletAddress.normalizeWallet()
        user.email = devAuth.email.trim().lowercase()
        user.displayName = devAuth.displayName.trim()
        user.status = UserStatus.ACTIVE
        user.roles = superRoles.toMutableSet()

        return userRepository.save(user)
    }

    private fun isEnabled(): Boolean =
        appProperties.devAuth.enabled && appProperties.devAuth.token.isNotBlank()

    private fun tokenMatches(token: String): Boolean =
        MessageDigest.isEqual(
            token.toByteArray(StandardCharsets.UTF_8),
            appProperties.devAuth.token.toByteArray(StandardCharsets.UTF_8),
        )

    private fun String.normalizeWallet(): String = trim().lowercase()
}
