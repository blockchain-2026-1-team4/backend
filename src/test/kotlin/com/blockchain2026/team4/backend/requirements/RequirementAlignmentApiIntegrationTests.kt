package com.blockchain2026.team4.backend.requirements

import com.blockchain2026.team4.backend.support.ApiIntegrationTestSupport
import com.blockchain2026.team4.backend.user.entity.UserRole
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

class RequirementAlignmentApiIntegrationTests : ApiIntegrationTestSupport() {
    @Test
    fun `notion and wireframe requirement APIs expose validator search transaction dispute and verification flows`() {
        val organizer = createUser(
            email = "requirement-organizer@example.com",
            walletAddress = "0x0000000000000000000000000000000000001001",
            roles = setOf(UserRole.USER, UserRole.ORGANIZER),
        )
        val buyer = createUser(
            email = "requirement-buyer@example.com",
            walletAddress = "0x0000000000000000000000000000000000001002",
        )
        val resaleBuyer = createUser(
            email = "requirement-resale-buyer@example.com",
            walletAddress = "0x0000000000000000000000000000000000001003",
        )
        val eventValidator = createUser(
            email = "event-validator@example.com",
            walletAddress = "0x0000000000000000000000000000000000001004",
        )
        val globalValidator = createUser(
            email = "global-validator@example.com",
            walletAddress = "0x0000000000000000000000000000000000001005",
        )
        val deletedUser = createUser(email = "delete-me@example.com")
        val admin = createUser(
            email = "requirement-admin@example.com",
            roles = setOf(UserRole.USER, UserRole.ADMIN),
        )

        mockMvc.perform(
            patch("/api/v1/users/${globalValidator.id}/validator")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.roles").isArray())

        mockMvc.perform(
            patch("/api/v1/users/${deletedUser.id}/delete")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("DELETED"))

        val now = Instant.now()
        val eventResult = mockMvc.perform(
            post("/api/v1/events")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(organizer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Requirement Alignment Concert",
                      "description": "검색 가능한 블록체인 티켓 행사",
                      "category": "CONCERT",
                      "venue": "Requirement Hall",
                      "imageUrl": null,
                      "eventAt": "${iso(now.plusSeconds(604800))}",
                      "ticketPriceWei": 100000000000000000,
                      "totalTicketCount": 3,
                      "primarySaleStart": "${iso(now.minusSeconds(60))}",
                      "primarySaleEnd": "${iso(now.plusSeconds(86400))}",
                      "resaleAllowed": true,
                      "maxResalePriceRate": 12000,
                      "resaleStart": "${iso(now.minusSeconds(30))}",
                      "resaleEnd": "${iso(now.plusSeconds(172800))}"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.remainingTicketCount").value(3))
            .andExpect(jsonPath("$.data.soldTicketCount").value(0))
            .andReturn()

        val eventId = readString(eventResult, "$.data.id")

        mockMvc.perform(
            patch("/api/v1/events/$eventId/resale-policy")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(organizer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "resaleAllowed": true,
                      "maxResalePriceRate": 15000,
                      "resaleStart": "${iso(now.minusSeconds(30))}",
                      "resaleEnd": "${iso(now.plusSeconds(259200))}"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.maxResalePriceRate").value(15000))

        mockMvc.perform(
            get("/api/v1/events")
                .queryParam("category", "CONCERT")
                .queryParam("query", "Alignment"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(eventId))

        mockMvc.perform(
            get("/api/v1/admin/events")
                .queryParam("query", "Requirement")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalElements").value(1))

        mockMvc.perform(
            patch("/api/v1/admin/events/$eventId/flag")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.flagged").value(true))

        mockMvc.perform(
            patch("/api/v1/admin/events/$eventId/unflag")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.flagged").value(false))

        mockMvc.perform(
            post("/api/v1/events/$eventId/validators")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(organizer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId": "${eventValidator.id}"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.eventId").value(eventId))
            .andExpect(jsonPath("$.data.validatorId").value(eventValidator.id.toString()))

        mockMvc.perform(
            get("/api/v1/events/$eventId/validators")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(organizer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].validatorId").value(eventValidator.id.toString()))

        val issueResult = mockMvc.perform(
            post("/api/v1/events/$eventId/tickets")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(organizer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"seatInfos": ["R-1", "R-2"]}"""),
        )
            .andExpect(status().isOk)
            .andReturn()

        val checkInTicketId = readString(issueResult, "$.data[0].id")
        val resaleTicketId = readString(issueResult, "$.data[1].id")

        mockMvc.perform(get("/api/v1/tickets/$checkInTicketId/validity"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.valid").value(false))
            .andExpect(jsonPath("$.data.reason").value("아직 판매되지 않은 티켓입니다."))

        mockMvc.perform(
            post("/api/v1/tickets/$checkInTicketId/purchase")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.ownerWalletAddress").value(buyer.walletAddress))

        mockMvc.perform(
            post("/api/v1/tickets/$resaleTicketId/purchase")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer)),
        )
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/v1/events/$eventId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.remainingTicketCount").value(1))
            .andExpect(jsonPath("$.data.soldTicketCount").value(2))

        mockMvc.perform(get("/api/v1/tickets/$checkInTicketId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.ownerWalletAddress").value(buyer.walletAddress))

        mockMvc.perform(get("/api/v1/wallets/${buyer.walletAddress}/tickets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(2))

        mockMvc.perform(get("/api/v1/tickets/$checkInTicketId/validity"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.valid").value(true))

        val expiresAt = now.plusSeconds(3600)
        mockMvc.perform(
            get("/api/v1/tickets/$checkInTicketId/check-in-message")
                .queryParam("claimedOwner", buyer.walletAddress!!)
                .queryParam("expiresAt", iso(expiresAt)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.contractTokenId").isNotEmpty())
            .andExpect(jsonPath("$.data.messageHash").isNotEmpty())

        mockMvc.perform(
            post("/api/v1/tickets/$checkInTicketId/qr")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "claimedOwner": "${buyer.walletAddress}",
                      "expiresAt": "${iso(expiresAt)}",
                      "signature": "0xrequirement-signature"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.contractTokenId").isNotEmpty())
            .andExpect(jsonPath("$.data.barcodeText").isNotEmpty())

        mockMvc.perform(
            post("/api/v1/check-ins")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(eventValidator))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "ticketId": "$checkInTicketId",
                      "claimedOwner": "${buyer.walletAddress}",
                      "expiresAt": "${iso(expiresAt)}",
                      "signature": "0xrequirement-signature",
                      "memo": "이벤트별 검증자 입장 처리"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.result").value("SUCCESS"))

        val listingResult = mockMvc.perform(
            post("/api/v1/tickets/$resaleTicketId/resale-listing")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"priceWei": 120000000000000000}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andReturn()

        val listingId = readString(listingResult, "$.data.id")

        mockMvc.perform(
            post("/api/v1/resale-listings/$listingId/purchase")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(resaleBuyer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("SOLD"))
            .andExpect(jsonPath("$.data.buyerId").value(resaleBuyer.id.toString()))
            .andExpect(jsonPath("$.data.purchasedAt").exists())

        mockMvc.perform(
            get("/api/v1/admin/resale-transactions")
                .queryParam("status", "SOLD")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.items[0].buyerId").value(resaleBuyer.id.toString()))

        val disputeResult = mockMvc.perform(
            post("/api/v1/disputes")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(resaleBuyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "resaleListingId": "$listingId",
                      "ticketId": "$resaleTicketId",
                      "type": "TICKET_NOT_DELIVERED",
                      "description": "리셀 구매 후 티켓 전달 상태 확인이 필요합니다."
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andReturn()

        val disputeId = readString(disputeResult, "$.data.id")

        mockMvc.perform(
            get("/api/v1/disputes/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(resaleBuyer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalElements").value(1))

        mockMvc.perform(
            get("/api/v1/admin/disputes")
                .queryParam("status", "OPEN")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalElements").value(1))

        mockMvc.perform(
            patch("/api/v1/admin/disputes/$disputeId/review")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "status": "RESOLVED",
                      "resolutionNote": "관리자가 거래 이력을 확인했습니다."
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("RESOLVED"))
            .andExpect(jsonPath("$.data.reviewedBy").value(admin.id.toString()))
    }
}
