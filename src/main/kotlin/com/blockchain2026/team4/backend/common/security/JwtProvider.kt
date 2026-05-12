package com.blockchain2026.team4.backend.common.security

import com.blockchain2026.team4.backend.common.config.AppProperties
import com.blockchain2026.team4.backend.user.dto.UserDto
import com.blockchain2026.team4.backend.user.entity.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val appProperties: AppProperties,
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(appProperties.jwt.secret.toByteArray(StandardCharsets.UTF_8))

    fun issueAccessToken(user: UserDto): String = issueToken(user, appProperties.jwt.accessTokenTtl.seconds)

    fun issueRefreshToken(user: UserDto): String = issueToken(user, appProperties.jwt.refreshTokenTtl.seconds)

    fun parse(token: String): AuthPrincipal {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(appProperties.jwt.issuer)
            .build()
            .parseSignedClaims(token)
            .payload

        return AuthPrincipal(
            userId = UUID.fromString(claims.subject),
            roles = claims.roles(),
            walletAddress = claims["walletAddress"] as? String,
            email = claims["email"] as? String,
        )
    }

    private fun issueToken(user: UserDto, ttlSeconds: Long): String {
        val now = Instant.now()
        return Jwts.builder()
            .issuer(appProperties.jwt.issuer)
            .subject(user.id.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(ttlSeconds)))
            .claim("roles", user.roles.map { it.name })
            .claim("walletAddress", user.walletAddress)
            .claim("email", user.email)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    private fun Claims.roles(): Set<UserRole> {
        val rawRoles = this["roles"] as? Collection<*> ?: return emptySet()
        return rawRoles.mapNotNull { role -> role?.toString()?.let(UserRole::valueOf) }.toSet()
    }
}
