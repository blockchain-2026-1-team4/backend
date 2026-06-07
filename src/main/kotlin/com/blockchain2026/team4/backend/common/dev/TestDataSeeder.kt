package com.blockchain2026.team4.backend.common.dev

import com.blockchain2026.team4.backend.common.config.AppProperties
import com.blockchain2026.team4.backend.dispute.entity.DisputeEntity
import com.blockchain2026.team4.backend.dispute.entity.DisputeStatus
import com.blockchain2026.team4.backend.dispute.entity.DisputeType
import com.blockchain2026.team4.backend.dispute.repository.DisputeRepository
import com.blockchain2026.team4.backend.event.entity.EventEntity
import com.blockchain2026.team4.backend.event.entity.EventStatus
import com.blockchain2026.team4.backend.event.repository.EventRepository
import com.blockchain2026.team4.backend.resale.entity.ResaleListingEntity
import com.blockchain2026.team4.backend.resale.entity.ResaleListingStatus
import com.blockchain2026.team4.backend.resale.repository.ResaleListingRepository
import com.blockchain2026.team4.backend.ticket.entity.TicketEntity
import com.blockchain2026.team4.backend.ticket.entity.TicketStatus
import com.blockchain2026.team4.backend.ticket.repository.TicketRepository
import com.blockchain2026.team4.backend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.imageio.ImageIO

@Component
@Order(2)
class TestDataSeeder(
    private val appProperties: AppProperties,
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val ticketRepository: TicketRepository,
    private val resaleListingRepository: ResaleListingRepository,
    private val disputeRepository: DisputeRepository,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(TestDataSeeder::class.java)

    override fun run(args: ApplicationArguments) {
        if (!appProperties.devData.seed) return
        if (!appProperties.devAuth.enabled) return
        if (eventRepository.count() > 0) {
            log.info("[TestDataSeeder] events already exist — skipping seed")
            return
        }

        val organizer = userRepository.findById(appProperties.devAuth.userId).orElse(null)
        if (organizer == null) {
            log.warn("[TestDataSeeder] dev user not found — skipping seed")
            return
        }

        val storageDir = Path.of(appProperties.storage.imageDirectory).toAbsolutePath()
        Files.createDirectories(storageDir)
        val urlPrefix = appProperties.storage.publicUrlPrefix.trimEnd('/')

        fun wei(eth: Double): BigInteger =
            BigDecimal.valueOf(eth).multiply(BigDecimal.TEN.pow(18)).toBigInteger()

        fun past(days: Long): Instant = Instant.now().minus(days, ChronoUnit.DAYS)
        fun future(days: Long): Instant = Instant.now().plus(days, ChronoUnit.DAYS)

        fun makeImage(filename: String, c1: Color, c2: Color, line1: String, line2: String = ""): String {
            val img = BufferedImage(800, 450, BufferedImage.TYPE_INT_RGB)
            val g = img.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.paint = GradientPaint(0f, 0f, c1, 800f, 450f, c2)
            g.fillRect(0, 0, 800, 450)
            g.color = Color(0, 0, 0, 90)
            g.fillRect(0, 0, 800, 450)
            g.color = Color(255, 255, 255, 230)
            g.font = Font("SansSerif", Font.BOLD, 38)
            val fm1 = g.fontMetrics
            val y1 = if (line2.isBlank()) 225 + fm1.ascent / 2 else 200
            g.drawString(line1, (800 - fm1.stringWidth(line1)) / 2, y1)
            if (line2.isNotBlank()) {
                g.font = Font("SansSerif", Font.PLAIN, 24)
                val fm2 = g.fontMetrics
                g.color = Color(255, 255, 255, 180)
                g.drawString(line2, (800 - fm2.stringWidth(line2)) / 2, y1 + 44)
            }
            g.dispose()
            ImageIO.write(img, "png", storageDir.resolve(filename).toFile())
            return "$urlPrefix/$filename"
        }

        // ── Event 1: BTS World Tour (PUBLISHED · MUSIC · 판매중) ─────────────────
        val e1img = makeImage("event-bts-tour.png",
            Color(0x1A, 0x1A, 0x2E), Color(0x8B, 0x5C, 0xF6),
            "BTS World Tour 2026", "Seoul Olympic Main Stadium")
        val e1 = eventRepository.save(EventEntity(
            organizer = organizer,
            contractEventId = BigInteger.valueOf(1),
            name = "BTS World Tour 2026 - Seoul",
            description = "세계 최대 K-POP 그룹 BTS의 월드투어 서울 공연. 올림픽 주경기장에서 만나는 특별한 밤. 총 10만 석 규모의 역대 최대 단독 공연.",
            category = "MUSIC",
            venue = "서울 올림픽 주경기장",
            imageUrl = e1img,
            eventAt = future(100),
            eventStartAt = future(100),
            eventEndAt = future(100).plus(3, ChronoUnit.HOURS),
            ticketPriceWei = wei(0.1),
            totalTicketCount = 6,
            remainingTicketCount = 2,
            soldTicketCount = 4,
            primarySaleStart = past(10),
            primarySaleEnd = future(90),
            resaleAllowed = true,
            maxResalePriceRate = 130,
            resaleStart = past(5),
            resaleEnd = future(95),
            status = EventStatus.PUBLISHED,
        ))

        // 6 tickets: AVAILABLE × 2, SOLD × 2, LISTED × 1, USED × 1
        val t1vip = ticketRepository.save(TicketEntity(
            event = e1, contractTokenId = BigInteger.valueOf(1001),
            seatInfo = "VIP-1", sectionName = "VIP",
            originalPriceWei = wei(0.15), saleStartAt = past(10), saleEndAt = future(90),
            resaleEnabled = true, resaleCapRate = 13000, status = TicketStatus.AVAILABLE,
        ))
        ticketRepository.save(TicketEntity(
            event = e1, contractTokenId = BigInteger.valueOf(1002),
            seatInfo = "A-1", sectionName = "A",
            originalPriceWei = wei(0.1), saleStartAt = past(10), saleEndAt = future(90),
            resaleEnabled = true, resaleCapRate = 13000, status = TicketStatus.AVAILABLE,
        ))
        ticketRepository.save(TicketEntity(
            event = e1, owner = organizer, contractTokenId = BigInteger.valueOf(1003),
            seatInfo = "A-2", sectionName = "A",
            originalPriceWei = wei(0.1), saleStartAt = past(10), saleEndAt = future(90),
            resaleEnabled = true, resaleCapRate = 13000, status = TicketStatus.SOLD,
        ))
        ticketRepository.save(TicketEntity(
            event = e1, owner = organizer, contractTokenId = BigInteger.valueOf(1004),
            seatInfo = "B-1", sectionName = "B",
            originalPriceWei = wei(0.08), saleStartAt = past(10), saleEndAt = future(90),
            resaleEnabled = true, resaleCapRate = 13000, status = TicketStatus.SOLD,
        ))
        val t1listed = ticketRepository.save(TicketEntity(
            event = e1, owner = organizer, contractTokenId = BigInteger.valueOf(1005),
            seatInfo = "B-2", sectionName = "B",
            originalPriceWei = wei(0.08), saleStartAt = past(10), saleEndAt = future(90),
            resaleEnabled = true, resaleCapRate = 13000, status = TicketStatus.LISTED,
        ))
        ticketRepository.save(TicketEntity(
            event = e1, owner = organizer, contractTokenId = BigInteger.valueOf(1006),
            seatInfo = "C-1", sectionName = "C",
            originalPriceWei = wei(0.07), saleStartAt = past(10), saleEndAt = future(90),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.USED,
            usedAt = past(1),
        ))

        // ResaleListing for B-2 (LISTED) + Dispute on it
        val rl1 = resaleListingRepository.save(ResaleListingEntity(
            ticket = t1listed, seller = organizer,
            priceWei = wei(0.095), status = ResaleListingStatus.ACTIVE,
        ))
        disputeRepository.save(DisputeEntity(
            reporter = organizer, resaleListing = rl1, ticket = t1listed,
            type = DisputeType.PAYMENT_ISSUE,
            description = "리셀 구매 후 결제 처리 중 오류가 발생했습니다. 토큰 ID 1005 티켓 B-2 구매 완료 여부 확인 요청.",
            status = DisputeStatus.OPEN,
        ))

        // ── Event 2: Seoul Tech Summit (PUBLISHED · CONFERENCE · 판매 예정) ──────
        val e2img = makeImage("event-tech-summit.png",
            Color(0x0C, 0x44, 0x7C), Color(0x06, 0xB6, 0xD4),
            "Seoul Tech Summit 2026", "COEX Convention Center")
        val e2 = eventRepository.save(EventEntity(
            organizer = organizer,
            contractEventId = BigInteger.valueOf(2),
            name = "Seoul Tech Summit 2026",
            description = "아시아 최대 테크 컨퍼런스. AI, 블록체인, 웹3 분야 글로벌 스피커들이 총출동합니다. 스타트업 네트워킹 세션 포함.",
            category = "CONFERENCE",
            venue = "코엑스 컨벤션센터, 서울",
            imageUrl = e2img,
            eventAt = future(74),
            eventStartAt = future(74),
            eventEndAt = future(74).plus(8, ChronoUnit.HOURS),
            ticketPriceWei = wei(0.03),
            totalTicketCount = 5,
            remainingTicketCount = 3,
            soldTicketCount = 2,
            primarySaleStart = future(38),
            primarySaleEnd = future(70),
            resaleAllowed = false,
            maxResalePriceRate = 100,
            resaleStart = null,
            resaleEnd = null,
            status = EventStatus.PUBLISHED,
        ))

        // 5 tickets: AVAILABLE × 3, SOLD × 1, USED × 1
        ticketRepository.save(TicketEntity(
            event = e2, contractTokenId = BigInteger.valueOf(2001),
            seatInfo = "VIP-1", sectionName = "VIP",
            originalPriceWei = wei(0.08), saleStartAt = future(38), saleEndAt = future(70),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.AVAILABLE,
        ))
        ticketRepository.save(TicketEntity(
            event = e2, contractTokenId = BigInteger.valueOf(2002),
            seatInfo = "GEN-1", sectionName = "GENERAL",
            originalPriceWei = wei(0.03), saleStartAt = future(38), saleEndAt = future(70),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.AVAILABLE,
        ))
        ticketRepository.save(TicketEntity(
            event = e2, contractTokenId = BigInteger.valueOf(2003),
            seatInfo = "GEN-2", sectionName = "GENERAL",
            originalPriceWei = wei(0.03), saleStartAt = future(38), saleEndAt = future(70),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.AVAILABLE,
        ))
        ticketRepository.save(TicketEntity(
            event = e2, owner = organizer, contractTokenId = BigInteger.valueOf(2004),
            seatInfo = "SPEAKER-1", sectionName = "SPEAKER",
            originalPriceWei = wei(0.03), saleStartAt = future(38), saleEndAt = future(70),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.SOLD,
        ))
        ticketRepository.save(TicketEntity(
            event = e2, owner = organizer, contractTokenId = BigInteger.valueOf(2005),
            seatInfo = "SPEAKER-2", sectionName = "SPEAKER",
            originalPriceWei = wei(0.03), saleStartAt = future(38), saleEndAt = future(70),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.USED,
            usedAt = past(1),
        ))

        // ── Event 3: 부산국제영화제 (INACTIVE · FILM · 종료) ──────────────────────
        val e3img = makeImage("event-film-fest.png",
            Color(0x7C, 0x3A, 0xED), Color(0xDB, 0x27, 0x77),
            "부산국제영화제 2025", "Busan Cinema Center")
        val e3 = eventRepository.save(EventEntity(
            organizer = organizer,
            contractEventId = BigInteger.valueOf(3),
            name = "부산국제영화제 2025",
            description = "아시아 최대 국제영화제. 전 세계 70개국 300편 이상 작품 상영. 거장 감독의 마스터 클래스 및 GV 포함.",
            category = "FILM",
            venue = "부산 영화의전당",
            imageUrl = e3img,
            eventAt = past(245),
            eventStartAt = past(245),
            eventEndAt = past(235),
            ticketPriceWei = wei(0.015),
            totalTicketCount = 4,
            remainingTicketCount = 0,
            soldTicketCount = 4,
            primarySaleStart = past(280),
            primarySaleEnd = past(250),
            resaleAllowed = true,
            maxResalePriceRate = 110,
            resaleStart = past(275),
            resaleEnd = past(248),
            status = EventStatus.INACTIVE,
        ))

        // 4 tickets: USED × 2, SOLD × 1, CANCELLED × 1
        ticketRepository.save(TicketEntity(
            event = e3, owner = organizer, contractTokenId = BigInteger.valueOf(3001),
            seatInfo = "SCR-A1", sectionName = "SCREEN-A",
            originalPriceWei = wei(0.015), saleStartAt = past(280), saleEndAt = past(250),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.USED,
            usedAt = past(240),
        ))
        ticketRepository.save(TicketEntity(
            event = e3, owner = organizer, contractTokenId = BigInteger.valueOf(3002),
            seatInfo = "SCR-A2", sectionName = "SCREEN-A",
            originalPriceWei = wei(0.015), saleStartAt = past(280), saleEndAt = past(250),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.USED,
            usedAt = past(238),
        ))
        val t3sold = ticketRepository.save(TicketEntity(
            event = e3, owner = organizer, contractTokenId = BigInteger.valueOf(3003),
            seatInfo = "SCR-B1", sectionName = "SCREEN-B",
            originalPriceWei = wei(0.015), saleStartAt = past(280), saleEndAt = past(250),
            resaleEnabled = true, resaleCapRate = 11000, status = TicketStatus.SOLD,
        ))
        ticketRepository.save(TicketEntity(
            event = e3, contractTokenId = BigInteger.valueOf(3004),
            seatInfo = "SCR-C1", sectionName = "SCREEN-C",
            originalPriceWei = wei(0.015), saleStartAt = past(280), saleEndAt = past(250),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.CANCELLED,
        ))

        // Dispute — 티켓 미수령 신고 (REVIEWING)
        disputeRepository.save(DisputeEntity(
            reporter = organizer, resaleListing = null, ticket = t3sold,
            type = DisputeType.TICKET_NOT_DELIVERED,
            description = "SCR-B1 티켓을 리셀 구매 후 앱에서 확인되지 않습니다. 블록체인 토큰 ID 3003 전송 기록 확인 요청.",
            status = DisputeStatus.REVIEWING,
        ))

        // ── Event 4: 현대미술 특별전 (DRAFT · ART · 미공개) ─────────────────────
        val e4img = makeImage("event-art-expo.png",
            Color(0xF5, 0x9E, 0x0B), Color(0xEF, 0x44, 0x44),
            "현대미술 특별전", "국립현대미술관 서울관")
        val e4 = eventRepository.save(EventEntity(
            organizer = organizer,
            contractEventId = null,
            name = "현대미술 특별전 — 경계의 상상",
            description = "국내외 현대미술 작가 30인의 신작. 빛, 공간, 디지털의 경계를 탐구하는 몰입형 전시. 아직 공개되지 않은 드래프트 이벤트입니다.",
            category = "ART",
            venue = "국립현대미술관, 서울관",
            imageUrl = e4img,
            eventAt = future(157),
            eventStartAt = future(157),
            eventEndAt = future(157).plus(6, ChronoUnit.HOURS),
            ticketPriceWei = wei(0.02),
            totalTicketCount = 6,
            remainingTicketCount = 6,
            soldTicketCount = 0,
            primarySaleStart = future(117),
            primarySaleEnd = future(152),
            resaleAllowed = false,
            maxResalePriceRate = 100,
            resaleStart = null,
            resaleEnd = null,
            status = EventStatus.DRAFT,
        ))

        // 3 tickets: AVAILABLE × 3 (draft — not purchasable yet)
        ticketRepository.save(TicketEntity(
            event = e4, contractTokenId = BigInteger.valueOf(4001),
            seatInfo = "GAL-A1", sectionName = "GALLERY-A",
            originalPriceWei = wei(0.02), saleStartAt = future(117), saleEndAt = future(152),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.AVAILABLE,
        ))
        ticketRepository.save(TicketEntity(
            event = e4, contractTokenId = BigInteger.valueOf(4002),
            seatInfo = "GAL-A2", sectionName = "GALLERY-A",
            originalPriceWei = wei(0.02), saleStartAt = future(117), saleEndAt = future(152),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.AVAILABLE,
        ))
        ticketRepository.save(TicketEntity(
            event = e4, contractTokenId = BigInteger.valueOf(4003),
            seatInfo = "GAL-B1", sectionName = "GALLERY-B",
            originalPriceWei = wei(0.025), saleStartAt = future(117), saleEndAt = future(152),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.AVAILABLE,
        ))

        // ── Event 5: K리그 올스타전 (CANCELLED · SPORTS) ───────────────────────
        val e5img = makeImage("event-kleague.png",
            Color(0x06, 0x5F, 0x46), Color(0x0E, 0xA5, 0xE9),
            "K리그 올스타전 2026", "수원월드컵경기장")
        val e5 = eventRepository.save(EventEntity(
            organizer = organizer,
            contractEventId = BigInteger.valueOf(5),
            name = "K리그 올스타전 2026",
            description = "K리그 역대 최고 스타들의 드림팀 대결. 경기장 공사로 인해 행사가 취소되었습니다. 환불이 자동 처리됩니다.",
            category = "SPORTS",
            venue = "수원월드컵경기장",
            imageUrl = e5img,
            eventAt = future(38),
            eventStartAt = future(38),
            eventEndAt = future(38).plus(2, ChronoUnit.HOURS),
            ticketPriceWei = wei(0.05),
            totalTicketCount = 5,
            remainingTicketCount = 2,
            soldTicketCount = 3,
            primarySaleStart = past(10),
            primarySaleEnd = future(30),
            resaleAllowed = false,
            maxResalePriceRate = 100,
            resaleStart = null,
            resaleEnd = null,
            status = EventStatus.CANCELLED,
        ))

        // 5 tickets: CANCELLED × 3, AVAILABLE × 1 (미구매), SOLD × 1
        ticketRepository.save(TicketEntity(
            event = e5, contractTokenId = BigInteger.valueOf(5001),
            seatInfo = "SEC-A1", sectionName = "SEC-A",
            originalPriceWei = wei(0.05), saleStartAt = past(10), saleEndAt = future(30),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.CANCELLED,
        ))
        ticketRepository.save(TicketEntity(
            event = e5, contractTokenId = BigInteger.valueOf(5002),
            seatInfo = "SEC-A2", sectionName = "SEC-A",
            originalPriceWei = wei(0.05), saleStartAt = past(10), saleEndAt = future(30),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.CANCELLED,
        ))
        ticketRepository.save(TicketEntity(
            event = e5, owner = organizer, contractTokenId = BigInteger.valueOf(5003),
            seatInfo = "SEC-B1", sectionName = "SEC-B",
            originalPriceWei = wei(0.05), saleStartAt = past(10), saleEndAt = future(30),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.CANCELLED,
        ))
        ticketRepository.save(TicketEntity(
            event = e5, contractTokenId = BigInteger.valueOf(5004),
            seatInfo = "SEC-B2", sectionName = "SEC-B",
            originalPriceWei = wei(0.05), saleStartAt = past(10), saleEndAt = future(30),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.AVAILABLE,
        ))
        ticketRepository.save(TicketEntity(
            event = e5, owner = organizer, contractTokenId = BigInteger.valueOf(5005),
            seatInfo = "SEC-C1", sectionName = "SEC-C",
            originalPriceWei = wei(0.05), saleStartAt = past(10), saleEndAt = future(30),
            resaleEnabled = false, resaleCapRate = 10000, status = TicketStatus.SOLD,
        ))

        log.info("[TestDataSeeder] seeded 5 events · 21 tickets · 1 resale listing · 2 disputes · 5 images → {}", storageDir)
    }
}
