package com.blockchain2026.team4.backend.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigInteger
import java.time.Duration

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val jwt: Jwt = Jwt(),
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
        val networkName: String = "local-anvil",
        val rpcUrl: String = "http://127.0.0.1:8545",
        val chainId: Long = 31337,
        val contractAddress: String = "",
        val operatorPrivateKey: String = "",
        val gasPriceWei: BigInteger = BigInteger.valueOf(1_000_000_000L),
        val gasLimit: BigInteger = BigInteger.valueOf(6_500_000L),
    )
}
