package com.blockchain2026.team4.backend.ticket

import com.blockchain2026.team4.backend.support.ApiIntegrationTestSupport
import com.blockchain2026.team4.backend.user.entity.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

class TicketLifecycleApiIntegrationTests : ApiIntegrationTestSupport() {
    @Test
    fun `event ticket resale qr check-in and admin APIs support the full backend flow`() {
        val organizer = createUser(
            email = "organizer@example.com",
            walletAddress = "0x0000000000000000000000000000000000000100",
            displayName = "공연 주최자",
            roles = setOf(UserRole.USER, UserRole.ORGANIZER),
        )
        val buyer = createUser(
            email = "buyer@example.com",
            walletAddress = "0x0000000000000000000000000000000000000200",
            displayName = "1차 구매자",
        )
        val resaleBuyer = createUser(
            email = "resale-buyer@example.com",
            walletAddress = "0x0000000000000000000000000000000000000300",
            displayName = "리셀 구매자",
        )
        val validator = createUser(
            email = "validator@example.com",
            walletAddress = "0x0000000000000000000000000000000000000400",
            displayName = "입장 검증자",
            roles = setOf(UserRole.USER, UserRole.VALIDATOR),
        )
        val admin = createUser(
            email = "lifecycle-admin@example.com",
            displayName = "운영 관리자",
            roles = setOf(UserRole.USER, UserRole.ADMIN),
        )

        val now = Instant.now()
        val eventResult = mockMvc.perform(
            post("/api/v1/events")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(organizer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Kyunghee Blockchain Concert",
                      "description": "블록체인 티켓 기반 캠퍼스 콘서트",
                      "category": "CONCERT",
                      "venue": "Peace Hall",
                      "imageUrl": null,
                      "eventAt": "${iso(now.plusSeconds(604800))}",
                      "ticketPriceWei": 100000000000000000,
                      "totalTicketCount": 2,
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
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Kyunghee Blockchain Concert"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andReturn()

        val eventId = readString(eventResult, "$.data.id")

        mockMvc.perform(
            patch("/api/v1/events/$eventId")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(organizer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Kyunghee Trust Ticket Concert",
                      "description": "공식 리셀과 QR 체크인을 포함한 콘서트",
                      "category": "MUSIC",
                      "venue": "Grand Peace Hall",
                      "imageUrl": null,
                      "eventAt": "${iso(now.plusSeconds(691200))}"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Kyunghee Trust Ticket Concert"))
            .andExpect(jsonPath("$.data.venue").value("Grand Peace Hall"))

        val imageResult = mockMvc.perform(
            multipart("/api/v1/events/$eventId/image")
                .file(MockMultipartFile("file", "poster.png", "image/png", byteArrayOf(1, 2, 3, 4)))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(organizer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andReturn()

        assertThat(readString(imageResult, "$.data.imageUrl")).startsWith("/images/")

        mockMvc.perform(
            patch("/api/v1/events/$eventId/status")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(organizer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "INACTIVE"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("INACTIVE"))

        mockMvc.perform(
            patch("/api/v1/events/$eventId/status")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(organizer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "ACTIVE"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))

        mockMvc.perform(get("/api/v1/events").queryParam("status", "ACTIVE"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(1))

        mockMvc.perform(get("/api/v1/events/$eventId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(eventId))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))

        val issuedResult = mockMvc.perform(
            post("/api/v1/events/$eventId/tickets")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(organizer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"seatInfos": ["A-1", "A-2"]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].status").value("AVAILABLE"))
            .andExpect(jsonPath("$.data[1].status").value("AVAILABLE"))
            .andReturn()

        val firstTicketId = readString(issuedResult, "$.data[0].id")
        val secondTicketId = readString(issuedResult, "$.data[1].id")

        mockMvc.perform(get("/api/v1/events/$eventId/tickets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].eventId").value(eventId))
            .andExpect(jsonPath("$.data[1].eventId").value(eventId))

        mockMvc.perform(
            post("/api/v1/tickets/$firstTicketId/purchase")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.ownerId").value(buyer.id.toString()))
            .andExpect(jsonPath("$.data.status").value("SOLD"))

        mockMvc.perform(
            post("/api/v1/tickets/$secondTicketId/purchase")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.ownerId").value(buyer.id.toString()))
            .andExpect(jsonPath("$.data.status").value("SOLD"))

        mockMvc.perform(
            get("/api/v1/tickets/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2))

        mockMvc.perform(
            get("/api/v1/tickets/$firstTicketId")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.ownerId").value(buyer.id.toString()))

        val purchasedListingResult = mockMvc.perform(
            post("/api/v1/tickets/$firstTicketId/resale-listing")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"priceWei": 110000000000000000}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.ticketId").value(firstTicketId))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andReturn()

        val purchasedListingId = readString(purchasedListingResult, "$.data.id")

        mockMvc.perform(get("/api/v1/resale-listings"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(purchasedListingId))

        mockMvc.perform(get("/api/v1/resale-listings/$purchasedListingId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.ticketId").value(firstTicketId))
            .andExpect(jsonPath("$.data.priceWei").value(110000000000000000))

        mockMvc.perform(
            post("/api/v1/resale-listings/$purchasedListingId/purchase")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(resaleBuyer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("SOLD"))

        val canceledListingResult = mockMvc.perform(
            post("/api/v1/tickets/$secondTicketId/resale-listing")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"priceWei": 105000000000000000}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andReturn()

        val canceledListingId = readString(canceledListingResult, "$.data.id")

        mockMvc.perform(
            patch("/api/v1/resale-listings/$canceledListingId/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("CANCELED"))

        mockMvc.perform(
            get("/api/v1/tickets/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(resaleBuyer)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value(firstTicketId))
            .andExpect(jsonPath("$.data[0].status").value("SOLD"))

        val expiresAt = now.plusSeconds(3600)
        mockMvc.perform(
            post("/api/v1/tickets/$firstTicketId/qr")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(resaleBuyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "claimedOwner": "${resaleBuyer.walletAddress}",
                      "expiresAt": "${iso(expiresAt)}",
                      "signature": "0xtest-signature"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.ticketId").value(firstTicketId))
            .andExpect(jsonPath("$.data.payload").isNotEmpty())
            .andExpect(jsonPath("$.data.qrPngBase64").isNotEmpty())

        mockMvc.perform(
            post("/api/v1/check-ins")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(validator))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "ticketId": "$firstTicketId",
                      "claimedOwner": "${resaleBuyer.walletAddress}",
                      "expiresAt": "${iso(expiresAt)}",
                      "signature": "0xtest-signature",
                      "memo": "정문 입장"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.ticketId").value(firstTicketId))
            .andExpect(jsonPath("$.data.validatorId").value(validator.id.toString()))
            .andExpect(jsonPath("$.data.result").value("SUCCESS"))

        mockMvc.perform(
            get("/api/v1/tickets/$firstTicketId/check-ins")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(validator)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].result").value("SUCCESS"))
            .andExpect(jsonPath("$.data[0].memo").value("정문 입장"))

        mockMvc.perform(
            get("/api/v1/admin/dashboard")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.activeEventCount").value(1))
            .andExpect(jsonPath("$.data.usedTicketCount").value(1))
            .andExpect(jsonPath("$.data.activeResaleListingCount").value(0))

        mockMvc.perform(
            get("/api/v1/admin/blockchain-transactions")
                .queryParam("size", "5")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].action").value("createEvent"))
            .andExpect(jsonPath("$.data[0].status").value("SIMULATED"))
    }
}
