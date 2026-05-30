package com.blockchain2026.team4.backend.auth.service

import com.blockchain2026.team4.backend.auth.dto.AuthTokensDto
import com.blockchain2026.team4.backend.auth.dto.EmailLoginCommand
import com.blockchain2026.team4.backend.auth.dto.EmailRegisterCommand
import com.blockchain2026.team4.backend.auth.dto.WalletLoginCommand
import com.blockchain2026.team4.backend.auth.dto.WalletNonceDto
import com.blockchain2026.team4.backend.auth.entity.WalletLoginNonceEntity
import com.blockchain2026.team4.backend.auth.repository.WalletLoginNonceRepository
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.common.security.JwtProvider
import com.blockchain2026.team4.backend.user.dto.UserDto
import com.blockchain2026.team4.backend.user.entity.UserRole
import com.blockchain2026.team4.backend.user.service.UserService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class AuthService(
    private val walletLoginNonceRepository: WalletLoginNonceRepository,
    private val walletSignatureVerifier: WalletSignatureVerifier,
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun issueWalletNonce(walletAddress: String): WalletNonceDto {
        val normalizedWallet = walletAddress.normalizeWallet()
        val nonce = generateNonce()
        val expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES)
        val message = """
            TrustTicket 로그인 요청
            지갑: $normalizedWallet
            nonce: $nonce
            만료: $expiresAt
        """.trimIndent()

        walletLoginNonceRepository.save(
            WalletLoginNonceEntity(
                walletAddress = normalizedWallet,
                nonce = nonce,
                message = message,
                expiresAt = expiresAt,
            ),
        )

        return WalletNonceDto(normalizedWallet, nonce, message, expiresAt)
    }

    @Transactional
    fun walletLogin(command: WalletLoginCommand): AuthTokensDto {
        val nonceEntity = walletLoginNonceRepository.findByNonce(command.nonce)
            ?: throw BusinessException(ErrorCode.WALLET_SIGNATURE_INVALID)
        val normalizedWallet = command.walletAddress.normalizeWallet()

        if (nonceEntity.walletAddress != normalizedWallet || nonceEntity.consumedAt != null || nonceEntity.expiresAt.isBefore(Instant.now())) {
            throw BusinessException(ErrorCode.WALLET_SIGNATURE_INVALID)
        }
        if (!walletSignatureVerifier.verify(nonceEntity.message, command.signature, normalizedWallet)) {
            throw BusinessException(ErrorCode.WALLET_SIGNATURE_INVALID)
        }

        nonceEntity.consumedAt = Instant.now()
        val user = userService.getOrCreateWalletUser(normalizedWallet)
        return issueTokens(user)
    }

    @Transactional
    fun emailRegister(command: EmailRegisterCommand): AuthTokensDto {
        throw BusinessException(ErrorCode.FORBIDDEN, "이메일 회원가입은 지원하지 않습니다.")
    }

    @Transactional(readOnly = true)
    fun emailLogin(command: EmailLoginCommand): AuthTokensDto {
        val (user, passwordHash) = userService.findEmailLoginUser(command.email)
        if (UserRole.ADMIN !in user.roles) {
            throw BusinessException(ErrorCode.FORBIDDEN, "ADMIN 계정만 이메일 로그인이 가능합니다.")
        }
        if (!passwordEncoder.matches(command.password, passwordHash)) {
            throw BadCredentialsException("Invalid credentials")
        }
        return issueTokens(user)
    }

    private fun issueTokens(user: UserDto): AuthTokensDto =
        AuthTokensDto(
            accessToken = jwtProvider.issueAccessToken(user),
            refreshToken = jwtProvider.issueRefreshToken(user),
            user = user,
        )

    private fun generateNonce(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun String.normalizeWallet(): String = trim().lowercase()
}
