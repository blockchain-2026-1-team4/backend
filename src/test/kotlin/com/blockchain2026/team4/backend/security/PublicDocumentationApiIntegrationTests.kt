package com.blockchain2026.team4.backend.security

import com.blockchain2026.team4.backend.support.ApiIntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PublicDocumentationApiIntegrationTests : ApiIntegrationTestSupport() {
    @Test
    fun `swagger ui legacy entrypoint is public`() {
        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    fun `openapi spec is public`() {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
    }
}
