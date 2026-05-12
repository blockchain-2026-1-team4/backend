package com.blockchain2026.team4.backend.auth

import com.blockchain2026.team4.backend.support.ApiIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthApiIntegrationTests : ApiIntegrationTestSupport() {
    @Test
    fun `email auth APIs issue tokens in standardized envelopes`() {
        val registerResult = mockMvc.perform(
            post("/api/v1/auth/email/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "email-user@example.com",
                      "password": "$DEFAULT_PASSWORD",
                      "displayName": "이메일 사용자"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.user.email").value("email-user@example.com"))
            .andExpect(jsonPath("$.data.user.roles[0]").value("USER"))
            .andExpect(jsonPath("$.meta.requestId").exists())
            .andReturn()

        assertThat(readString(registerResult, "$.data.accessToken")).isNotBlank()
        assertThat(readString(registerResult, "$.data.refreshToken")).isNotBlank()

        mockMvc.perform(
            post("/api/v1/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "email-user@example.com",
                      "password": "$DEFAULT_PASSWORD"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.email").value("email-user@example.com"))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
    }

    @Test
    fun `wallet auth APIs verify signed nonce and create wallet user`() {
        val wallet = createWallet()
        val nonceResult = mockMvc.perform(
            post("/api/v1/auth/wallet/nonce")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"walletAddress": "${wallet.address.uppercase()}"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.walletAddress").value(wallet.address))
            .andExpect(jsonPath("$.data.nonce").isNotEmpty())
            .andExpect(jsonPath("$.data.message").isNotEmpty())
            .andReturn()

        val nonce = readString(nonceResult, "$.data.nonce")
        val message = readString(nonceResult, "$.data.message")
        val signature = signWalletMessage(message, wallet.keyPair)

        val loginResult = mockMvc.perform(
            post("/api/v1/auth/wallet/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "walletAddress": "${wallet.address}",
                      "nonce": "$nonce",
                      "signature": "$signature"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.walletAddress").value(wallet.address))
            .andExpect(jsonPath("$.data.user.roles[0]").value("USER"))
            .andReturn()

        assertThat(readString(loginResult, "$.data.accessToken")).isNotBlank()
        assertThat(userRepository.findByWalletAddressIgnoreCase(wallet.address)).isNotNull
    }

    @Test
    fun `auth APIs return standardized validation and credential errors`() {
        mockMvc.perform(
            post("/api/v1/auth/email/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "not-an-email",
                      "password": "short",
                      "displayName": "잘못된 사용자"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.path").value("/api/v1/auth/email/register"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0].field").isNotEmpty())
            .andExpect(jsonPath("$.stackTrace").isArray())
            .andExpect(jsonPath("$.meta.requestId").exists())

        mockMvc.perform(
            post("/api/v1/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "missing@example.com",
                      "password": "$DEFAULT_PASSWORD"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.path").value("/api/v1/auth/email/login"))
            .andExpect(jsonPath("$.stackTrace").isArray())
    }
}
