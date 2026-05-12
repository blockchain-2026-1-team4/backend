package com.blockchain2026.team4.backend.resale.facade

import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.resale.controller.request.ResaleCreateRequest
import com.blockchain2026.team4.backend.resale.controller.response.ResaleListingResponse
import com.blockchain2026.team4.backend.resale.dto.ResaleCreateCommand
import com.blockchain2026.team4.backend.resale.mapper.ResaleApiMapper
import com.blockchain2026.team4.backend.resale.service.ResaleService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ResaleFacade(
    private val resaleService: ResaleService,
    private val resaleApiMapper: ResaleApiMapper,
) {
    fun create(userId: UUID, ticketId: UUID, request: ResaleCreateRequest): ResaleListingResponse =
        resaleApiMapper.toResponse(resaleService.createListing(userId, ticketId, ResaleCreateCommand(request.priceWei)))

    fun purchase(userId: UUID, listingId: UUID): ResaleListingResponse =
        resaleApiMapper.toResponse(resaleService.purchaseListing(userId, listingId))

    fun cancel(userId: UUID, listingId: UUID): ResaleListingResponse =
        resaleApiMapper.toResponse(resaleService.cancelListing(userId, listingId))

    fun get(listingId: UUID): ResaleListingResponse = resaleApiMapper.toResponse(resaleService.get(listingId))

    fun list(page: Int, size: Int): PageResponse<ResaleListingResponse> {
        val listings = resaleService.list(page, size)
        return PageResponse(
            items = resaleApiMapper.toResponses(listings.items),
            page = listings.page,
            size = listings.size,
            totalElements = listings.totalElements,
            totalPages = listings.totalPages,
            hasNext = listings.hasNext,
        )
    }
}
