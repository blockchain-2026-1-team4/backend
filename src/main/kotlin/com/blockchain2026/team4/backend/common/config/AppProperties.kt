package com.blockchain2026.team4.backend.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigInteger
import java.time.Duration
import java.util.UUID

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val jwt: Jwt = Jwt(),
    val devAuth: DevAuth = DevAuth(),
    val errors: Errors = Errors(),
    val storage: Storage = Storage(),
    val blockchain: Blockchain = Blockchain(),
) {
    data class Jwt(
        val issuer: String = "blockchain-2026-team4",
        val secret: String = "local-development-secret-local-development-secret",
        val accessTokenExpirationMinutes: Long = 120,
        val refreshTokenExpirationDays: Long = 14,
    ) {
        val accessTokenTtl: Duration
            get() = Duration.ofMinutes(accessTokenExpirationMinutes)

        val refreshTokenTtl: Duration
            get() = Duration.ofDays(refreshTokenExpirationDays)
    }

    data class DevAuth(
        val enabled: Boolean = false,
        val token: String = "",
        val userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000004"),
        val walletAddress: String = "0x0000000000000000000000000000000000000004",
        val email: String = "dev-admin@local.test",
        val password: String = "Admin1234!",
        val displayName: String = "Local Dev Super Admin",
    )

    data class Errors(
        val includeStackTrace: Boolean = true,
        val stackTraceDepth: Int = 8,
    )

    data class Storage(
        val imageDirectory: String = "./storage/images",
        val publicUrlPrefix: String = "/images",
    )

    data class Blockchain(
        val enabled: Boolean = false,
        val networkName: String = "kaia-kairos-testnet",
        val rpcUrl: String = "https://public-en-kairos.node.kaia.io",
        val chainId: Long = 1001,
        val contractAddress: String = "0x3e1B4b3F8B61D12DFe7Ba1d8893Ff4E84bdb378C",
        val operatorPrivateKey: String = "",
        val gasPriceWei: BigInteger = BigInteger.valueOf(1_000_000_000L),
        val gasLimit: BigInteger = BigInteger.valueOf(6_500_000L),
    )
}
