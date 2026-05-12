package com.blockchain2026.team4.backend.organizer.facade

import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.organizer.controller.request.OrganizerApplicationRequest
import com.blockchain2026.team4.backend.organizer.controller.request.OrganizerReviewRequest
import com.blockchain2026.team4.backend.organizer.controller.response.OrganizerApplicationResponse
import com.blockchain2026.team4.backend.organizer.dto.OrganizerApplicationCommand
import com.blockchain2026.team4.backend.organizer.dto.OrganizerReviewCommand
import com.blockchain2026.team4.backend.organizer.entity.OrganizerApplicationStatus
import com.blockchain2026.team4.backend.organizer.mapper.OrganizerApplicationApiMapper
import com.blockchain2026.team4.backend.organizer.service.OrganizerApplicationService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizerApplicationFacade(
    private val organizerApplicationService: OrganizerApplicationService,
    private val organizerApplicationApiMapper: OrganizerApplicationApiMapper,
) {
    fun apply(userId: UUID, request: OrganizerApplicationRequest): OrganizerApplicationResponse =
        organizerApplicationApiMapper.toResponse(
            organizerApplicationService.apply(
                userId,
                OrganizerApplicationCommand(request.businessName, request.contactEmail, request.description),
            ),
        )

    fun review(adminId: UUID, applicationId: UUID, request: OrganizerReviewRequest): OrganizerApplicationResponse =
        organizerApplicationApiMapper.toResponse(
            organizerApplicationService.review(adminId, applicationId, OrganizerReviewCommand(request.status)),
        )

    fun mine(userId: UUID): List<OrganizerApplicationResponse> =
        organizerApplicationApiMapper.toResponses(organizerApplicationService.listMine(userId))

    fun list(status: OrganizerApplicationStatus?, page: Int, size: Int): PageResponse<OrganizerApplicationResponse> {
        val applications = organizerApplicationService.list(status, page, size)
        return PageResponse(
            items = organizerApplicationApiMapper.toResponses(applications.items),
            page = applications.page,
            size = applications.size,
            totalElements = applications.totalElements,
            totalPages = applications.totalPages,
            hasNext = applications.hasNext,
        )
    }
}
