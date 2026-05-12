package com.blockchain2026.team4.backend.user

import com.blockchain2026.team4.backend.support.ApiIntegrationTestSupport
import com.blockchain2026.team4.backend.user.entity.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserAndOrganizerApiIntegrationTests : ApiIntegrationTestSupport() {
    @Test
    fun `user profile and admin user APIs expose standardized management flows`() {
        val user = createUser(email = "profile@example.com", displayName = "기존 이름")
        val admin = createUser(
            email = "admin@example.com",
            displayName = "관리자",
            roles = setOf(UserRole.USER, UserRole.ADMIN),
        )

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(user.id.toString()))
            .andExpect(jsonPath("$.data.email").value("profile@example.com"))
            .andExpect(jsonPath("$.data.displayName").value("기존 이름"))

        mockMvc.perform(
            patch("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName": "수정된 이름"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.displayName").value("수정된 이름"))

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearerToken(admin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.items").isArray())

        mockMvc.perform(
            patch("/api/v1/users/${user.id}/suspend")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("SUSPENDED"))

        mockMvc.perform(
            patch("/api/v1/users/${user.id}/activate")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
    }

    @Test
    fun `organizer application APIs approve applicants and grant organizer access`() {
        val applicant = createUser(email = "applicant@example.com", displayName = "신청자")
        val admin = createUser(
            email = "organizer-admin@example.com",
            displayName = "승인 관리자",
            roles = setOf(UserRole.USER, UserRole.ADMIN),
        )

        val applicationResult = mockMvc.perform(
            post("/api/v1/organizer-applications")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "businessName": "Team4 Tickets",
                      "contactEmail": "owner@team4.test",
                      "description": "캠퍼스 공연 티켓 운영"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userId").value(applicant.id.toString()))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn()

        val applicationId = readString(applicationResult, "$.data.id")

        mockMvc.perform(
            get("/api/v1/organizer-applications/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(applicant)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value(applicationId))

        mockMvc.perform(
            get("/api/v1/organizer-applications")
                .queryParam("status", "PENDING")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(1))

        mockMvc.perform(
            patch("/api/v1/organizer-applications/$applicationId/review")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "APPROVED"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("APPROVED"))
            .andExpect(jsonPath("$.data.reviewedBy").value(admin.id.toString()))

        val approvedApplicant = userRepository.findById(applicant.id).orElseThrow()
        assertThat(approvedApplicant.roles).contains(UserRole.ORGANIZER)

        mockMvc.perform(
            get("/api/v1/events/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(approvedApplicant)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(0))
    }

    @Test
    fun `protected APIs return standardized security error envelopes`() {
        val user = createUser(email = "plain-user@example.com")

        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.path").value("/api/v1/users/me"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.stackTrace").isArray())
            .andExpect(jsonPath("$.meta.requestId").exists())

        mockMvc.perform(
            get("/api/v1/admin/dashboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.path").value("/api/v1/admin/dashboard"))

        mockMvc.perform(
            get("/api/v1/admin/dashboard")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(user)),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.path").value("/api/v1/admin/dashboard"))
            .andExpect(jsonPath("$.stackTrace").isArray())
    }
}
