package com.blockchain2026.team4.backend.security

import com.blockchain2026.team4.backend.support.ApiIntegrationTestSupport
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@TestPropertySource(
    properties = [
        "app.dev-auth.enabled=true",
        "app.dev-auth.token=test-dev-super-token",
    ],
)
class DevAuthenticationApiIntegrationTests : ApiIntegrationTestSupport() {
    @Test
    fun `dev token authenticates as all-role local user without signup`() {
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer test-dev-super-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("00000000-0000-0000-0000-000000000004"))
            .andExpect(jsonPath("$.data.email").value("dev-admin@local.test"))
            .andExpect(jsonPath("$.data.walletAddress").value("0x0000000000000000000000000000000000000004"))
            .andExpect(jsonPath("$.data.roles", containsInAnyOrder("USER", "ORGANIZER", "ADMIN", "VALIDATOR")))

        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer test-dev-super-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `wrong dev token remains unauthorized`() {
        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer wrong-token"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }
}
