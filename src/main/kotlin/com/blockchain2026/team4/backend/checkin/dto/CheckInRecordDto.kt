package com.blockchain2026.team4.backend.checkin.dto

import com.blockchain2026.team4.backend.checkin.entity.CheckInResult
import java.time.Instant
import java.util.UUID

data class CheckInRecordDto(
    val id: UUID,
    val ticketId: UUID,
    val validatorId: UUID,
    val result: CheckInResult,
    val checkedInAt: Instant,
    val memo: String?,
)
