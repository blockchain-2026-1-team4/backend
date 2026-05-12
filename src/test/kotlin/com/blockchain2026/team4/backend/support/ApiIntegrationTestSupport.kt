package com.blockchain2026.team4.backend.support

import com.blockchain2026.team4.backend.common.security.JwtProvider
import com.blockchain2026.team4.backend.user.dto.UserDto
import com.blockchain2026.team4.backend.user.entity.UserEntity
import com.blockchain2026.team4.backend.user.entity.UserRole
import com.blockchain2026.team4.backend.user.repository.UserRepository
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.nio.charset.StandardCharsets
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiIntegrationTestSupport {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun resetDatabase() {
        listOf(
            "disputes",
            "check_in_records",
            "resale_listings",
            "tickets",
            "event_validators",
            "events",
            "organizer_applications",
            "blockchain_transactions",
            "wallet_login_nonces",
            "user_roles",
            "users",
        ).forEach { jdbcTemplate.execute("delete from $it") }
    }

    protected fun createUser(
        email: String? = null,
        walletAddress: String? = null,
        password: String = DEFAULT_PASSWORD,
        displayName: String? = "테스트 사용자",
        roles: Set<UserRole> = setOf(UserRole.USER),
    ): UserEntity =
        userRepository.saveAndFlush(
            UserEntity(
                email = email,
                walletAddress = walletAddress?.lowercase(),
                passwordHash = email?.let { passwordEncoder.encode(password) },
                displayName = displayName,
                roles = roles.toMutableSet(),
            ),
        )

    protected fun bearerToken(user: UserEntity): String = "Bearer ${jwtProvider.issueAccessToken(user.toDto())}"

    protected fun readString(result: MvcResult, path: String): String =
        JsonPath.read(result.response.contentAsString, path)

    protected fun createWallet(): TestWallet {
        val keyPair = Keys.createEcKeyPair()
        return TestWallet(
            keyPair = keyPair,
            address = "0x${Keys.getAddress(keyPair)}".lowercase(),
        )
    }

    protected fun signWalletMessage(message: String, keyPair: ECKeyPair): String {
        val signature = Sign.signPrefixedMessage(message.toByteArray(StandardCharsets.UTF_8), keyPair)
        return Numeric.toHexString(signature.r + signature.s + signature.v)
    }

    protected fun iso(instant: Instant): String = instant.toString()

    protected data class TestWallet(
        val keyPair: ECKeyPair,
        val address: String,
    )

    private fun UserEntity.toDto(): UserDto =
        UserDto(
            id = id,
            walletAddress = walletAddress,
            email = email,
            displayName = displayName,
            status = status,
            roles = roles,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        const val DEFAULT_PASSWORD = "Passw0rd!2026"
    }
}
