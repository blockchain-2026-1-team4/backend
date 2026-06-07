package com.blockchain2026.team4.backend.common.dev

import com.blockchain2026.team4.backend.common.config.AppProperties
import com.blockchain2026.team4.backend.event.entity.EventEntity
import com.blockchain2026.team4.backend.event.entity.EventRoundEntity
import com.blockchain2026.team4.backend.event.entity.EventStatus
import com.blockchain2026.team4.backend.event.repository.EventRepository
import com.blockchain2026.team4.backend.event.repository.EventRoundRepository
import com.blockchain2026.team4.backend.ticket.entity.TicketEntity
import com.blockchain2026.team4.backend.ticket.entity.TicketStatus
import com.blockchain2026.team4.backend.ticket.repository.TicketRepository
import com.blockchain2026.team4.backend.user.entity.UserEntity
import com.blockchain2026.team4.backend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.imageio.ImageIO

@Component
@Order(2)
class TestDataSeeder(
    private val appProperties: AppProperties,
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val eventRoundRepository: EventRoundRepository,
    private val ticketRepository: TicketRepository,
    private val jdbcTemplate: JdbcTemplate,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(TestDataSeeder::class.java)

    /** 글로벌 NFT 토큰 ID 카운터 */
    private var tokenId = 100_001L

    override fun run(args: ApplicationArguments) {
        if (!appProperties.devData.seed) return
        if (!appProperties.devAuth.enabled) return

        val organizer = userRepository.findById(appProperties.devAuth.userId).orElse(null) ?: run {
            log.warn("[TestDataSeeder] dev user not found — skipping seed")
            return
        }

        // ── 기존 데이터 전체 삭제 (FK 의존 순서) ─────────────────────────────────
        log.info("[TestDataSeeder] clearing all existing business data…")
        jdbcTemplate.execute("DELETE FROM disputes")
        jdbcTemplate.execute("DELETE FROM resale_listings")
        jdbcTemplate.execute("DELETE FROM check_in_records")
        jdbcTemplate.execute("DELETE FROM blockchain_transactions")
        jdbcTemplate.execute("DELETE FROM tickets")
        jdbcTemplate.execute("DELETE FROM event_validators")
        jdbcTemplate.execute("DELETE FROM event_rounds")
        jdbcTemplate.execute("DELETE FROM events")

        val storageDir = Path.of(appProperties.storage.imageDirectory).toAbsolutePath()
        Files.createDirectories(storageDir)
        val urlPrefix = appProperties.storage.publicUrlPrefix.trimEnd('/')

        // ── 공통 헬퍼 ────────────────────────────────────────────────────────────

        fun wei(eth: Double): BigInteger =
            BigDecimal.valueOf(eth).multiply(BigDecimal.TEN.pow(18)).toBigInteger()

        fun past(days: Int): Instant = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)
        fun future(days: Int): Instant = Instant.now().plus(days.toLong(), ChronoUnit.DAYS)
        fun ld(offsetDays: Int): LocalDate = LocalDate.now().plusDays(offsetDays.toLong())
        /** 회차 시작 1시간 전(UTC) — 티켓 판매 마감 기본값 */
        fun saleEndFor(daysOffset: Int, startHour: Int): Instant =
            LocalDate.now().plusDays(daysOffset.toLong())
                .atTime(startHour, 0).toInstant(ZoneOffset.UTC)
                .minus(1, ChronoUnit.HOURS)

        /** 그라디언트 포스터 이미지 생성 */
        fun makeImage(filename: String, c1: Color, c2: Color, line1: String, line2: String = ""): String {
            val img = BufferedImage(800, 450, BufferedImage.TYPE_INT_RGB)
            val g = img.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.paint = GradientPaint(0f, 0f, c1, 800f, 450f, c2)
            g.fillRect(0, 0, 800, 450)
            g.color = Color(0, 0, 0, 90)
            g.fillRect(0, 0, 800, 450)
            g.color = Color(255, 255, 255, 235)
            g.font = Font("SansSerif", Font.BOLD, 38)
            val fm1 = g.fontMetrics
            val y1 = if (line2.isBlank()) 225 + fm1.ascent / 2 else 195
            g.drawString(line1, (800 - fm1.stringWidth(line1)) / 2, y1)
            if (line2.isNotBlank()) {
                g.font = Font("SansSerif", Font.PLAIN, 22)
                val fm2 = g.fontMetrics
                g.color = Color(255, 255, 255, 175)
                g.drawString(line2, (800 - fm2.stringWidth(line2)) / 2, y1 + 48)
            }
            g.dispose()
            ImageIO.write(img, "png", storageDir.resolve(filename).toFile())
            return "$urlPrefix/$filename"
        }

        /** EventEntity 저장 */
        fun saveEvent(
            contractId: Long?,
            name: String,
            desc: String,
            category: String,
            venue: String,
            img: String,
            startAt: Instant,
            endAt: Instant,
            total: Int,
            remaining: Int,
            sold: Int,
            saleStart: Instant,
            saleEnd: Instant,
            status: EventStatus,
            resaleAllowed: Boolean = false,
            priceWei: BigInteger = wei(0.05),
        ): EventEntity = eventRepository.save(EventEntity(
            organizer = organizer,
            contractEventId = contractId?.let { BigInteger.valueOf(it) },
            name = name,
            description = desc,
            category = category,
            venue = venue,
            imageUrl = img,
            eventAt = startAt,
            eventStartAt = startAt,
            eventEndAt = endAt,
            ticketPriceWei = priceWei,
            totalTicketCount = total,
            remainingTicketCount = remaining,
            soldTicketCount = sold,
            primarySaleStart = saleStart,
            primarySaleEnd = saleEnd,
            resaleAllowed = resaleAllowed,
            maxResalePriceRate = if (resaleAllowed) 130 else 100,
            resaleStart = if (resaleAllowed) saleStart else null,
            resaleEnd = if (resaleAllowed) saleEnd else null,
            status = status,
        ))

        /** EventRoundEntity 저장 */
        fun saveRound(
            evt: EventEntity,
            num: Int,
            eventDate: LocalDate,
            startHour: Int,
            endHour: Int,
            saleStart: Instant,
            saleEnd: Instant,
        ): EventRoundEntity = eventRoundRepository.save(EventRoundEntity(
            event = evt,
            title = "${num}회차",
            eventDate = eventDate,
            startTime = LocalTime.of(startHour, 0),
            endTime = LocalTime.of(endHour, 0),
            saleStartAt = saleStart,
            saleEndAt = saleEnd,
            useGlobalSalePeriod = false,
        ))

        /**
         * 회차 티켓 목록 생성.
         * sections = Triple(섹션명, 수량, 상태). 섹션명은 회차 내에서 유일해야 함.
         * vararg를 먼저 두고 선택적 usedAt는 named arg로 전달.
         * SOLD/USED/CANCELLED 티켓의 owner는 항상 organizer.
         */
        fun buildTickets(
            evt: EventEntity,
            rnd: EventRoundEntity,
            priceWei: BigInteger,
            saleStart: Instant,
            saleEnd: Instant,
            vararg sections: Triple<String, Int, TicketStatus>,
            usedAt: Instant? = null,
        ): List<TicketEntity> = sections.flatMap { (section, count, status) ->
            (1..count).map { n ->
                TicketEntity(
                    event = evt,
                    owner = if (status == TicketStatus.AVAILABLE) null else organizer,
                    contractTokenId = BigInteger.valueOf(tokenId++),
                    seatInfo = "$section-${n.toString().padStart(3, '0')}",
                    sectionName = section,
                    eventRoundId = rnd.id,
                    originalPriceWei = priceWei,
                    saleStartAt = saleStart,
                    saleEndAt = saleEnd,
                    resaleEnabled = false,
                    resaleCapRate = 10_000,
                    status = status,
                    usedAt = if (status == TicketStatus.USED) (usedAt ?: past(1)) else null,
                )
            }
        }

        fun saveTickets(vararg batches: List<TicketEntity>): Int {
            val all = batches.flatMap { it }
            ticketRepository.saveAll(all)
            return all.size
        }

        val A = TicketStatus.AVAILABLE
        val S = TicketStatus.SOLD
        val U = TicketStatus.USED
        val C = TicketStatus.CANCELLED

        var totalCount = 0

        // ══════════════════════════════════════════════════════════════════════════
        // E01  여의도 봄꽃 콘서트
        // R1: 종료·전원입장(USED) / R2: 종료·전원입장(USED) / R3: 판매중·잔여
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-01-spring-concert.png",
                Color(0xFF, 0x6B, 0x9D), Color(0x8B, 0x1A, 0x5E),
                "여의도 봄꽃 콘서트", "여의도 한강공원 야외무대")
            val price = wei(0.08)
            val e = saveEvent(1, "여의도 봄꽃 콘서트 2026",
                "벚꽃이 만개한 한강공원 야외무대에서 펼쳐지는 감성 콘서트. 3회차 시리즈 공연.",
                "CONCERT", "여의도 한강공원 야외무대", img,
                past(64).plus(19, ChronoUnit.HOURS), past(64).plus(22, ChronoUnit.HOURS),
                total = 600, remaining = 150, sold = 450,
                saleStart = past(95), saleEnd = future(36),
                status = EventStatus.PUBLISHED, resaleAllowed = true, priceWei = price)
            val r1 = saveRound(e, 1, ld(-64), 19, 22, past(95),  saleEndFor(-64, 19))
            val r2 = saveRound(e, 2, ld(-29), 19, 22, past(68),  saleEndFor(-29, 19))
            val r3 = saveRound(e, 3, ld(37),  19, 22, past(7),   saleEndFor(37, 19))
            totalCount += saveTickets(
                buildTickets(e, r1, price, past(95), saleEndFor(-64, 19),
                    Triple("VIP", 20, U), Triple("A", 80, U),
                    usedAt = past(63)),
                buildTickets(e, r2, price, past(68), saleEndFor(-29, 19),
                    Triple("VIP", 30, U), Triple("A", 100, U), Triple("B", 70, U),
                    usedAt = past(28)),
                // R3: 150 AVAILABLE + 150 SOLD (섹션명 중복 없이)
                buildTickets(e, r3, price, past(7), saleEndFor(37, 19),
                    Triple("VIP", 50, A), Triple("A", 100, A), Triple("B", 150, S)),
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E02  서울 여름 콘서트
        // R1: 판매중·잔여 / R2: 판매 예정·발행 완료 / R3: 미발행
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-02-summer-concert.png",
                Color(0xFF, 0x8C, 0x00), Color(0xFF, 0x45, 0x00),
                "서울 여름 콘서트", "올림픽공원 잔디마당")
            val price = wei(0.10)
            val e = saveEvent(2, "서울 여름 콘서트 2026",
                "뜨거운 여름 밤, 올림픽공원 야외에서 만나는 여름 특별 콘서트.",
                "CONCERT", "올림픽공원 잔디마당, 서울", img,
                future(23).plus(19, ChronoUnit.HOURS), future(84).plus(22, ChronoUnit.HOURS),
                total = 300, remaining = 260, sold = 40,
                saleStart = past(38), saleEnd = future(83),
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(23), 19, 22, past(38),   saleEndFor(23, 19))
            val r2 = saveRound(e, 2, ld(53), 19, 22, future(23), saleEndFor(53, 19))
            val r3 = saveRound(e, 3, ld(84), 19, 22, future(53), saleEndFor(84, 19))
            totalCount += saveTickets(
                // R1: 60 AVAILABLE + 40 SOLD = 100
                buildTickets(e, r1, price, past(38), saleEndFor(23, 19),
                    Triple("VIP", 20, A), Triple("A", 40, A), Triple("B", 40, S)),
                // R2: 200 AVAILABLE (판매 예정, 발행은 완료)
                buildTickets(e, r2, price, future(23), saleEndFor(53, 19),
                    Triple("VIP", 30, A), Triple("A", 100, A), Triple("B", 70, A)),
                // R3 미발행: 티켓 없음
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E03  부산 록 페스티벌
        // R1·R2·R3: 판매중 + 전체 매진(SOLD)
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-03-rock-fest.png",
                Color(0x1A, 0x00, 0x30), Color(0xC7, 0x00, 0x2E),
                "부산 록 페스티벌", "부산 BEXCO 야외광장")
            val price = wei(0.12)
            val e = saveEvent(3, "부산 록 페스티벌 2026",
                "국내외 록 밴드들이 총출동하는 부산 대형 야외 페스티벌. 3회차 전 회차 매진.",
                "FESTIVAL", "부산 BEXCO 야외광장", img,
                future(68).plus(16, ChronoUnit.HOURS), future(128).plus(23, ChronoUnit.HOURS),
                total = 600, remaining = 0, sold = 600,
                saleStart = past(30), saleEnd = future(127),
                status = EventStatus.PUBLISHED, resaleAllowed = true, priceWei = price)
            val r1 = saveRound(e, 1, ld(68),  16, 23, past(30),  saleEndFor(68, 16))
            val r2 = saveRound(e, 2, ld(98),  16, 23, past(20),  saleEndFor(98, 16))
            val r3 = saveRound(e, 3, ld(128), 16, 23, past(10),  saleEndFor(128, 16))
            totalCount += saveTickets(
                buildTickets(e, r1, price, past(30), saleEndFor(68, 16),
                    Triple("VIP", 20, S), Triple("A", 80, S)),
                buildTickets(e, r2, price, past(20), saleEndFor(98, 16),
                    Triple("VIP", 30, S), Triple("A", 100, S), Triple("B", 70, S)),
                buildTickets(e, r3, price, past(10), saleEndFor(128, 16),
                    Triple("VIP", 50, S), Triple("A", 150, S), Triple("B", 100, S)),
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E04  대구 클래식 콘서트
        // R1·R2·R3: 판매 예정(아직 판매 기간 전), 티켓 발행 완료
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-04-classic.png",
                Color(0x0C, 0x26, 0x5E), Color(0x14, 0x7F, 0xB5),
                "대구 클래식 콘서트", "대구 오페라하우스")
            val price = wei(0.06)
            val e = saveEvent(4, "대구 클래식 음악 시리즈 2026",
                "세계 정상급 오케스트라와 함께하는 대구 클래식 음악 시리즈. 3개월에 걸친 세 번의 공연.",
                "CONCERT", "대구 오페라하우스", img,
                future(85).plus(19, ChronoUnit.HOURS), future(145).plus(22, ChronoUnit.HOURS),
                total = 600, remaining = 600, sold = 0,
                saleStart = future(54), saleEnd = future(144),
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(85),  19, 22, future(54),  saleEndFor(85, 19))
            val r2 = saveRound(e, 2, ld(115), 19, 22, future(85),  saleEndFor(115, 19))
            val r3 = saveRound(e, 3, ld(145), 19, 22, future(115), saleEndFor(145, 19))
            totalCount += saveTickets(
                buildTickets(e, r1, price, future(54),  saleEndFor(85, 19),
                    Triple("VIP", 20, A), Triple("A", 80, A)),
                buildTickets(e, r2, price, future(85),  saleEndFor(115, 19),
                    Triple("VIP", 30, A), Triple("A", 100, A), Triple("B", 70, A)),
                buildTickets(e, r3, price, future(115), saleEndFor(145, 19),
                    Triple("VIP", 50, A), Triple("A", 150, A), Triple("B", 100, A)),
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E05  인천 K-POP 페스트
        // R1: 종료·전원입장 / R2: 판매중(오늘 공연) / R3: 미발행
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-05-kpop.png",
                Color(0x6B, 0x21, 0xA8), Color(0xEC, 0x48, 0x99),
                "인천 K-POP 페스트", "인천 송도 컨벤시아")
            val price = wei(0.09)
            val todaySaleEnd = future(1)
            val e = saveEvent(5, "인천 K-POP 페스티벌 2026",
                "국내 최대 K-POP 신인 발굴 축제. 아이돌 스타부터 신예 아티스트까지.",
                "FESTIVAL", "인천 송도 컨벤시아 야외광장", img,
                past(19).plus(18, ChronoUnit.HOURS), future(42).plus(22, ChronoUnit.HOURS),
                total = 300, remaining = 100, sold = 200,
                saleStart = past(68), saleEnd = todaySaleEnd,
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(-19), 18, 22, past(68),   saleEndFor(-19, 18))
            val r2 = saveRound(e, 2, ld(0),   18, 22, past(38),   saleEndFor(0, 18))  // 오늘 18:00 한 시간 전
            val r3 = saveRound(e, 3, ld(42),  19, 22, future(12), saleEndFor(42, 19))
            totalCount += saveTickets(
                buildTickets(e, r1, price, past(68), saleEndFor(-19, 18),
                    Triple("VIP", 20, U), Triple("A", 80, U),
                    usedAt = past(18)),
                // R2: 100 AVAILABLE + 100 SOLD (섹션명 구분)
                buildTickets(e, r2, price, past(38), saleEndFor(0, 18),
                    Triple("VIP", 30, A), Triple("A", 70, A), Triple("B", 100, S)),
                // R3 미발행
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E06  광주 비엔날레
        // R1: 판매중·매진 / R2: 판매중·잔여 / R3: 미발행
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-06-biennale.png",
                Color(0x14, 0x53, 0x2D), Color(0x15, 0xBB, 0x8F),
                "광주 비엔날레", "국립아시아문화전당")
            val price = wei(0.04)
            val e = saveEvent(6, "광주 비엔날레 2026",
                "현대미술의 현재와 미래를 탐구하는 광주 비엔날레 특별전. 전 세계 60개국 아티스트 참여.",
                "EXHIBITION", "국립아시아문화전당, 광주", img,
                future(12).plus(10, ChronoUnit.HOURS), future(72).plus(19, ChronoUnit.HOURS),
                total = 300, remaining = 100, sold = 200,
                saleStart = past(38), saleEnd = future(71),
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(12), 10, 19, past(38),   saleEndFor(12, 10))
            val r2 = saveRound(e, 2, ld(42), 10, 19, past(7),    saleEndFor(42, 10))
            val r3 = saveRound(e, 3, ld(72), 10, 19, future(37), saleEndFor(72, 10))
            totalCount += saveTickets(
                // R1: 100 SOLD (매진)
                buildTickets(e, r1, price, past(38), saleEndFor(12, 10),
                    Triple("A", 60, S), Triple("B", 40, S)),
                // R2: 100 AVAILABLE + 100 SOLD
                buildTickets(e, r2, price, past(7), saleEndFor(42, 10),
                    Triple("A", 100, A), Triple("B", 70, S), Triple("C", 30, A)),
                // R3 미발행
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E07  제주 음악 축제
        // R1·R2·R3: 연말 예정, 판매 예정, 티켓 전량 발행 완료
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-07-jeju-music.png",
                Color(0x06, 0x6B, 0xB2), Color(0x2D, 0xD4, 0xBF),
                "제주 음악 축제", "제주 탐라문화광장")
            val price = wei(0.07)
            val e = saveEvent(7, "제주 국제 음악 축제 2026",
                "제주 한라산을 배경으로 펼쳐지는 연말 음악 축제. 재즈·클래식·월드뮤직 3가지 테마.",
                "FESTIVAL", "제주 탐라문화광장", img,
                future(176).plus(18, ChronoUnit.HOURS), future(190).plus(22, ChronoUnit.HOURS),
                total = 600, remaining = 600, sold = 0,
                saleStart = future(145), saleEnd = future(189),
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(176), 18, 22, future(145), saleEndFor(176, 18))
            val r2 = saveRound(e, 2, ld(183), 18, 22, future(145), saleEndFor(183, 18))
            val r3 = saveRound(e, 3, ld(190), 18, 22, future(145), saleEndFor(190, 18))
            totalCount += saveTickets(
                buildTickets(e, r1, price, future(145), saleEndFor(176, 18),
                    Triple("VIP", 20, A), Triple("A", 80, A)),
                buildTickets(e, r2, price, future(145), saleEndFor(183, 18),
                    Triple("VIP", 30, A), Triple("A", 100, A), Triple("B", 70, A)),
                buildTickets(e, r3, price, future(145), saleEndFor(190, 18),
                    Triple("VIP", 50, A), Triple("A", 150, A), Triple("B", 100, A)),
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E08  서울 AI 컨퍼런스
        // R1·R2·R3: 모두 종료 (전원 USED)
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-08-ai-conf.png",
                Color(0x0F, 0x17, 0x2A), Color(0x38, 0xBD, 0xF8),
                "서울 AI 컨퍼런스", "코엑스 컨벤션센터")
            val price = wei(0.03)
            val e = saveEvent(8, "서울 AI & 블록체인 컨퍼런스 2026",
                "AI·블록체인 분야 글로벌 전문가 100인이 모이는 연례 컨퍼런스. 올해 3회 모두 종료.",
                "CONFERENCE", "코엑스 컨벤션센터, 서울", img,
                past(85).plus(9, ChronoUnit.HOURS), past(25).plus(18, ChronoUnit.HOURS),
                total = 600, remaining = 0, sold = 600,
                saleStart = past(145), saleEnd = past(26),
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(-85), 9, 18, past(145), saleEndFor(-85, 9))
            val r2 = saveRound(e, 2, ld(-55), 9, 18, past(115), saleEndFor(-55, 9))
            val r3 = saveRound(e, 3, ld(-25), 9, 18, past(85),  saleEndFor(-25, 9))
            totalCount += saveTickets(
                buildTickets(e, r1, price, past(145), saleEndFor(-85, 9),
                    Triple("GEN", 60, U), Triple("VIP", 20, U), Triple("SPK", 20, U),
                    usedAt = past(84)),
                buildTickets(e, r2, price, past(115), saleEndFor(-55, 9),
                    Triple("GEN", 120, U), Triple("VIP", 40, U), Triple("SPK", 40, U),
                    usedAt = past(54)),
                buildTickets(e, r3, price, past(85), saleEndFor(-25, 9),
                    Triple("GEN", 180, U), Triple("VIP", 60, U), Triple("SPK", 60, U),
                    usedAt = past(24)),
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E09  수원 스포츠 데이
        // R1: 종료 / R2: 판매중·매진 / R3: 미발행
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-09-sports-day.png",
                Color(0x14, 0x53, 0x2D), Color(0xCA, 0x8A, 0x04),
                "수원 스포츠 데이", "수원월드컵경기장")
            val price = wei(0.05)
            val e = saveEvent(9, "수원 스포츠 데이 2026",
                "K리그·농구·배구 복합 스포츠 축제. 3회차 진행 중 2회차는 매진.",
                "SPORTS", "수원월드컵경기장", img,
                past(49).plus(14, ChronoUnit.HOURS), future(73).plus(18, ChronoUnit.HOURS),
                total = 300, remaining = 0, sold = 300,
                saleStart = past(100), saleEnd = future(72),
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(-49), 14, 18, past(100), saleEndFor(-49, 14))
            val r2 = saveRound(e, 2, ld(17),  14, 18, past(38),  saleEndFor(17, 14))
            val r3 = saveRound(e, 3, ld(73),  14, 18, future(17), saleEndFor(73, 14))
            totalCount += saveTickets(
                buildTickets(e, r1, price, past(100), saleEndFor(-49, 14),
                    Triple("GEN-U", 50, U), Triple("GEN-S", 50, S),
                    usedAt = past(48)),
                buildTickets(e, r2, price, past(38), saleEndFor(17, 14),
                    Triple("GEN", 120, S), Triple("VIP", 80, S)),
                // R3 미발행
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E10  전주 국제영화제
        // R1: 판매중·잔여 / R2: 판매 예정·발행 완료 / R3: 미발행
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-10-film-fest.png",
                Color(0x78, 0x35, 0x0F), Color(0xF9, 0x73, 0x16),
                "전주 국제영화제", "전주 영화의거리")
            val price = wei(0.03)
            val e = saveEvent(10, "전주 국제영화제 2026",
                "독립·예술 영화의 중심 전주에서 열리는 국제영화제. 50개국 200여 편 상영.",
                "FILM", "전주 영화의거리 일대", img,
                future(22).plus(10, ChronoUnit.HOURS), future(82).plus(23, ChronoUnit.HOURS),
                total = 300, remaining = 260, sold = 40,
                saleStart = past(24), saleEnd = future(81),
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(22), 10, 23, past(24),   saleEndFor(22, 10))
            val r2 = saveRound(e, 2, ld(52), 10, 23, future(22), saleEndFor(52, 10))
            val r3 = saveRound(e, 3, ld(82), 10, 23, future(52), saleEndFor(82, 10))
            totalCount += saveTickets(
                // R1: 60 AVAILABLE + 40 SOLD = 100
                buildTickets(e, r1, price, past(24), saleEndFor(22, 10),
                    Triple("SCR-A", 60, A), Triple("SCR-B", 40, S)),
                // R2: 200 AVAILABLE
                buildTickets(e, r2, price, future(22), saleEndFor(52, 10),
                    Triple("SCR-A", 80, A), Triple("SCR-B", 80, A), Triple("SCR-C", 40, A)),
                // R3 미발행
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E11  대전 EDM 페스트
        // R1: 판매중·매진 / R2: 판매 예정·발행 완료 / R3: 미발행
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-11-edm.png",
                Color(0x3B, 0x00, 0x6B), Color(0x7C, 0x3A, 0xED),
                "대전 EDM 페스트", "대전 엑스포시민광장")
            val price = wei(0.11)
            val e = saveEvent(11, "대전 EDM 페스티벌 2026",
                "DJ 크루와 함께하는 대전 최대 일렉트로닉 댄스 뮤직 페스티벌.",
                "FESTIVAL", "대전 엑스포시민광장", img,
                future(32).plus(16, ChronoUnit.HOURS), future(92).plus(24, ChronoUnit.HOURS),
                total = 300, remaining = 200, sold = 100,
                saleStart = past(30), saleEnd = future(91),
                status = EventStatus.PUBLISHED, resaleAllowed = true, priceWei = price)
            val r1 = saveRound(e, 1, ld(32), 16, 23, past(30),   saleEndFor(32, 16))
            val r2 = saveRound(e, 2, ld(62), 16, 23, future(32), saleEndFor(62, 16))
            val r3 = saveRound(e, 3, ld(92), 16, 23, future(62), saleEndFor(92, 16))
            totalCount += saveTickets(
                // R1: 100 SOLD (매진)
                buildTickets(e, r1, price, past(30), saleEndFor(32, 16),
                    Triple("GEN", 60, S), Triple("VIP", 40, S)),
                // R2: 200 AVAILABLE (판매 예정, 발행 완료)
                buildTickets(e, r2, price, future(32), saleEndFor(62, 16),
                    Triple("GEN", 130, A), Triple("VIP", 70, A)),
                // R3 미발행
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E12  울산 마라톤
        // R1: 종료 / R2: 종료 / R3: 판매중(오늘 대회)·잔여
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-12-marathon.png",
                Color(0xEA, 0x58, 0x0C), Color(0xFB, 0xBF, 0x24),
                "울산 마라톤", "울산대공원 출발점")
            val price = wei(0.02)
            val e = saveEvent(12, "울산 국제 마라톤 2026",
                "울산 대공원을 출발점으로 하는 국제 마라톤 대회. 풀·하프·10K 세 가지 코스.",
                "SPORTS", "울산대공원, 울산", img,
                past(99).plus(6, ChronoUnit.HOURS), past(99).plus(12, ChronoUnit.HOURS),
                total = 600, remaining = 150, sold = 450,
                saleStart = past(160), saleEnd = future(1),
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(-99), 6, 12, past(160), saleEndFor(-99, 6))
            val r2 = saveRound(e, 2, ld(-69), 6, 12, past(130), saleEndFor(-69, 6))
            val r3 = saveRound(e, 3, ld(0),   6, 12, past(30),  saleEndFor(0, 6))  // 오늘 06:00 한 시간 전 마감
            totalCount += saveTickets(
                buildTickets(e, r1, price, past(160), saleEndFor(-99, 6),
                    Triple("FULL", 30, U), Triple("HALF", 40, U), Triple("10K", 30, U),
                    usedAt = past(98)),
                buildTickets(e, r2, price, past(130), saleEndFor(-69, 6),
                    Triple("FULL", 60, U), Triple("HALF", 80, U), Triple("10K", 60, U),
                    usedAt = past(68)),
                // R3: 150 AVAILABLE + 150 SOLD = 300
                buildTickets(e, r3, price, past(30), saleEndFor(0, 6),
                    Triple("FULL", 80, A), Triple("HALF", 70, A), Triple("10K", 150, S)),
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E13  경주 역사 문화제
        // R1: 종료·전원입장 / R2: 판매 기간 종료·이벤트 미래 / R3: 미발행
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-13-gyeongju.png",
                Color(0x7C, 0x2D, 0x12), Color(0x92, 0x40, 0x0E),
                "경주 역사 문화제", "경주 첨성대 광장")
            val price = wei(0.03)
            val e = saveEvent(13, "경주 역사 문화제 2026",
                "신라 천년의 역사를 체험하는 경주 대표 문화 축제. R2는 판매 종료, R3는 미발행.",
                "ETC", "경주 첨성대 광장", img,
                past(38).plus(10, ChronoUnit.HOURS), future(37).plus(20, ChronoUnit.HOURS),
                total = 300, remaining = 200, sold = 100,
                saleStart = past(99), saleEnd = future(36),
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(-38), 10, 20, past(99),  saleEndFor(-38, 10))
            val r2 = saveRound(e, 2, ld(7),   10, 20, past(68),  past(1))   // 판매 종료(어제), 공연은 미래 — 의도적 조기 마감
            val r3 = saveRound(e, 3, ld(37),  10, 20, future(7), saleEndFor(37, 10))
            totalCount += saveTickets(
                buildTickets(e, r1, price, past(99), saleEndFor(-38, 10),
                    Triple("GEN", 60, U), Triple("VIP", 40, U),
                    usedAt = past(37)),
                // R2: 200 AVAILABLE (판매 종료로 구매 불가지만 티켓은 있음)
                buildTickets(e, r2, price, past(68), past(1),
                    Triple("GEN", 130, A), Triple("VIP", 70, A)),
                // R3 미발행
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E14  창원 항구 페스트
        // R1: 종료(USED+SOLD) / R2: 오늘부터 판매 시작 / R3: 판매 예정·발행
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-14-changwon.png",
                Color(0x0E, 0x7A, 0x9E), Color(0x67, 0xE8, 0xF9),
                "창원 항구 페스트", "창원 진해 군항제")
            val price = wei(0.05)
            val todaySaleStart = Instant.now().truncatedTo(ChronoUnit.HOURS)
            val e = saveEvent(14, "창원 항구 페스티벌 2026",
                "창원 진해 군항제에서 펼쳐지는 항구 축제. R2 오늘부터 판매 시작.",
                "FESTIVAL", "창원 진해 군항 특설무대", img,
                past(3).plus(15, ChronoUnit.HOURS), future(57).plus(21, ChronoUnit.HOURS),
                total = 600, remaining = 500, sold = 100,
                saleStart = past(38), saleEnd = future(56),
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(-3), 15, 21, past(38),      saleEndFor(-3, 15))
            val r2 = saveRound(e, 2, ld(27), 15, 21, todaySaleStart, saleEndFor(27, 15))
            val r3 = saveRound(e, 3, ld(57), 15, 21, future(27),    saleEndFor(57, 15))
            totalCount += saveTickets(
                // R1: 80 USED + 20 SOLD = 100
                buildTickets(e, r1, price, past(38), saleEndFor(-3, 15),
                    Triple("GEN-U", 80, U), Triple("VIP-S", 20, S),
                    usedAt = past(2)),
                buildTickets(e, r2, price, todaySaleStart, saleEndFor(27, 15),
                    Triple("GEN", 120, A), Triple("VIP", 50, A), Triple("B", 30, A)),
                buildTickets(e, r3, price, future(27), saleEndFor(57, 15),
                    Triple("GEN", 180, A), Triple("VIP", 80, A), Triple("B", 40, A)),
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E15  강릉 해변 축제
        // R1·R2·R3: 판매중 + 전체 매진
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-15-beach-fest.png",
                Color(0x0D, 0x47, 0x8A), Color(0xF9, 0xA8, 0x25),
                "강릉 해변 축제", "강릉 경포해변")
            val price = wei(0.08)
            val e = saveEvent(15, "강릉 해변 여름 축제 2026",
                "동해 최대 해변 축제. 버스킹·푸드트럭·불꽃 쇼까지. 전 회차 매진 행렬.",
                "FESTIVAL", "강릉 경포해변 특설무대", img,
                future(42).plus(15, ChronoUnit.HOURS), future(56).plus(23, ChronoUnit.HOURS),
                total = 600, remaining = 0, sold = 600,
                saleStart = past(15), saleEnd = future(55),
                status = EventStatus.PUBLISHED, resaleAllowed = true, priceWei = price)
            val r1 = saveRound(e, 1, ld(42), 15, 23, past(15), saleEndFor(42, 15))
            val r2 = saveRound(e, 2, ld(49), 15, 23, past(10), saleEndFor(49, 15))
            val r3 = saveRound(e, 3, ld(56), 15, 23, past(5),  saleEndFor(56, 15))
            totalCount += saveTickets(
                buildTickets(e, r1, price, past(15), saleEndFor(42, 15),
                    Triple("VIP", 20, S), Triple("A", 80, S)),
                buildTickets(e, r2, price, past(10), saleEndFor(49, 15),
                    Triple("VIP", 30, S), Triple("A", 100, S), Triple("B", 70, S)),
                buildTickets(e, r3, price, past(5), saleEndFor(56, 15),
                    Triple("VIP", 50, S), Triple("A", 150, S), Triple("B", 100, S)),
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E16  춘천 레이크 페스트
        // R1: 판매 예정·발행 / R2: 미발행 / R3: 미발행
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-16-lake-fest.png",
                Color(0x16, 0x5A, 0x72), Color(0x34, 0xD3, 0x99),
                "춘천 레이크 페스트", "의암호 수변공원")
            val price = wei(0.06)
            val e = saveEvent(16, "춘천 레이크 페스티벌 2026",
                "의암호 수변공원에서 펼쳐지는 가을 레이크 페스티벌. R1만 발행, R2·R3는 준비중.",
                "FESTIVAL", "의암호 수변공원, 춘천", img,
                future(124).plus(12, ChronoUnit.HOURS), future(138).plus(20, ChronoUnit.HOURS),
                total = 100, remaining = 100, sold = 0,
                saleStart = future(93), saleEnd = future(137),
                status = EventStatus.PUBLISHED, priceWei = price)
            val r1 = saveRound(e, 1, ld(124), 12, 20, future(93),  saleEndFor(124, 12))
            val r2 = saveRound(e, 2, ld(131), 12, 20, future(100), saleEndFor(131, 12))
            val r3 = saveRound(e, 3, ld(138), 12, 20, future(107), saleEndFor(138, 12))
            totalCount += saveTickets(
                buildTickets(e, r1, price, future(93), saleEndFor(124, 12),
                    Triple("GEN", 70, A), Triple("VIP", 30, A)),
                // R2, R3 미발행
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E17  세종 시민 콘서트  (INACTIVE)
        // R1: 종료·전원입장 / R2: 종료·일부입장 / R3: 판매 종료·잔여 있음
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-17-sejong.png",
                Color(0x37, 0x47, 0x51), Color(0x94, 0xA3, 0xB8),
                "세종 시민 콘서트", "세종 호수공원 야외무대")
            val price = wei(0.02)
            val e = saveEvent(17, "세종 시민 콘서트 2026",
                "세종시민을 위한 무료 콘서트. 현재 비공개 상태로 전환되어 있습니다.",
                "CONCERT", "세종 호수공원 야외무대", img,
                past(59).plus(18, ChronoUnit.HOURS), future(1).plus(21, ChronoUnit.HOURS),
                total = 600, remaining = 200, sold = 400,
                saleStart = past(120), saleEnd = past(2),
                status = EventStatus.INACTIVE, priceWei = price)
            val r1 = saveRound(e, 1, ld(-59), 18, 21, past(120), saleEndFor(-59, 18))
            val r2 = saveRound(e, 2, ld(-29), 18, 21, past(90),  saleEndFor(-29, 18))
            val r3 = saveRound(e, 3, ld(1),   18, 21, past(60),  past(2))  // 판매 종료(이틀 전), 공연 내일 — 의도적 조기 마감
            totalCount += saveTickets(
                buildTickets(e, r1, price, past(120), saleEndFor(-59, 18),
                    Triple("GEN", 60, U), Triple("VIP", 20, U), Triple("B", 20, U),
                    usedAt = past(58)),
                buildTickets(e, r2, price, past(90), saleEndFor(-29, 18),
                    Triple("GEN-U", 90, U), Triple("VIP-U", 30, U), Triple("B-U", 30, U), Triple("GEN-S", 50, S),
                    usedAt = past(28)),
                // R3: 200 AVAILABLE + 100 SOLD = 300
                buildTickets(e, r3, price, past(60), past(2),
                    Triple("GEN", 150, A), Triple("VIP", 50, S), Triple("B", 100, S)),
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E18  포항 철강 박람회  (INACTIVE · 완전 종료된 2025년 행사)
        // R1·R2·R3: 모두 종료·전원입장
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-18-pohang.png",
                Color(0x78, 0x71, 0x6C), Color(0xF9, 0x73, 0x16),
                "포항 철강 박람회", "포항 POSCO 광장")
            val price = wei(0.02)
            val e = saveEvent(18, "포항 철강 산업 박람회 2025",
                "국내 철강 산업의 과거와 미래를 한 눈에 볼 수 있는 대형 박람회. 2025년 10월 종료.",
                "ETC", "포항 POSCO 광장", img,
                past(251).plus(9, ChronoUnit.HOURS), past(222).plus(18, ChronoUnit.HOURS),
                total = 600, remaining = 0, sold = 600,
                saleStart = past(370), saleEnd = past(223),
                status = EventStatus.INACTIVE, priceWei = price)
            val r1 = saveRound(e, 1, ld(-251), 9, 18, past(370), saleEndFor(-251, 9))
            val r2 = saveRound(e, 2, ld(-237), 9, 18, past(356), saleEndFor(-237, 9))
            val r3 = saveRound(e, 3, ld(-222), 9, 18, past(341), saleEndFor(-222, 9))
            totalCount += saveTickets(
                buildTickets(e, r1, price, past(370), saleEndFor(-251, 9),
                    Triple("GEN", 60, U), Triple("VIP", 20, U), Triple("EXH", 20, U),
                    usedAt = past(250)),
                buildTickets(e, r2, price, past(356), saleEndFor(-237, 9),
                    Triple("GEN", 120, U), Triple("VIP", 40, U), Triple("EXH", 40, U),
                    usedAt = past(236)),
                buildTickets(e, r3, price, past(341), saleEndFor(-222, 9),
                    Triple("GEN", 180, U), Triple("VIP", 60, U), Triple("EXH", 60, U),
                    usedAt = past(221)),
            )
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E19  안동 민속 대축제  (DRAFT · 초안)
        // R1·R2·R3: 계획은 있으나 티켓 미발행
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-19-andong.png",
                Color(0x7C, 0x1D, 0x1D), Color(0xCA, 0x8A, 0x04),
                "안동 민속 대축제", "안동 하회마을")
            val e = saveEvent(null, "안동 민속 대축제 2026",
                "유네스코 세계유산 하회마을을 배경으로 펼쳐지는 전통 민속 축제. 기획 중인 초안 이벤트.",
                "ETC", "안동 하회마을 특설무대", img,
                future(119).plus(10, ChronoUnit.HOURS), future(121).plus(20, ChronoUnit.HOURS),
                total = 0, remaining = 0, sold = 0,
                saleStart = future(88), saleEnd = future(120),
                status = EventStatus.DRAFT)
            // 라운드는 기획 단계로 생성, 티켓은 미발행
            saveRound(e, 1, ld(119), 10, 20, future(88), saleEndFor(119, 10))
            saveRound(e, 2, ld(120), 10, 20, future(88), saleEndFor(120, 10))
            saveRound(e, 3, ld(121), 10, 20, future(88), saleEndFor(121, 10))
        }

        // ══════════════════════════════════════════════════════════════════════════
        // E20  고양 킨텍스 박람회  (CANCELLED · 취소)
        // R1·R2: CANCELLED 티켓 / R3: 미발행
        // ══════════════════════════════════════════════════════════════════════════
        run {
            val img = makeImage("evt-20-kintex.png",
                Color(0x1E, 0x3A, 0x5F), Color(0x93, 0xC5, 0xFD),
                "고양 킨텍스 박람회", "킨텍스 제2전시장")
            val price = wei(0.04)
            val e = saveEvent(19, "고양 킨텍스 국제 박람회 2026",
                "킨텍스 제2전시장에서 열릴 예정이었던 대형 박람회. 시설 공사로 인해 전면 취소.",
                "ETC", "킨텍스 제2전시장, 고양", img,
                future(23).plus(10, ChronoUnit.HOURS), future(83).plus(18, ChronoUnit.HOURS),
                total = 300, remaining = 0, sold = 0,
                saleStart = past(30), saleEnd = future(82),
                status = EventStatus.CANCELLED, priceWei = price)
            val r1 = saveRound(e, 1, ld(23), 10, 18, past(30),   saleEndFor(23, 10))
            val r2 = saveRound(e, 2, ld(53), 10, 18, future(23), saleEndFor(53, 10))
            val r3 = saveRound(e, 3, ld(83), 10, 18, future(53), saleEndFor(83, 10))
            totalCount += saveTickets(
                buildTickets(e, r1, price, past(30), saleEndFor(23, 10),
                    Triple("GEN", 60, C), Triple("VIP", 40, C)),
                buildTickets(e, r2, price, future(23), saleEndFor(53, 10),
                    Triple("GEN", 120, C), Triple("VIP", 80, C)),
                // R3 미발행 (취소 공지 전 미발행 상태)
            )
        }

        log.info(
            "[TestDataSeeder] seeded 20 events · 60 rounds · {} tickets · 20 poster images → {}",
            totalCount, storageDir,
        )
    }
}
