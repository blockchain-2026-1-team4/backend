package com.blockchain2026.team4.backend.user.facade

import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.user.controller.request.UpdateMeRequest
import com.blockchain2026.team4.backend.user.controller.response.UserResponse
import com.blockchain2026.team4.backend.user.dto.UserUpdateCommand
import com.blockchain2026.team4.backend.user.entity.UserRole
import com.blockchain2026.team4.backend.user.entity.UserStatus
import com.blockchain2026.team4.backend.user.mapper.UserApiMapper
import com.blockchain2026.team4.backend.user.service.UserService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserFacade(
    private val userService: UserService,
    private val userApiMapper: UserApiMapper,
) {
    fun me(userId: UUID): UserResponse = userApiMapper.toResponse(userService.getUser(userId))

    fun updateMe(userId: UUID, request: UpdateMeRequest): UserResponse =
        userApiMapper.toResponse(userService.updateMe(userId, UserUpdateCommand(request.displayName)))

    fun listUsers(page: Int, size: Int, status: UserStatus?): PageResponse<UserResponse> {
        val users = userService.listUsers(page, size, status)
        return PageResponse(
            items = userApiMapper.toResponses(users.items),
            page = users.page,
            size = users.size,
            totalElements = users.totalElements,
            totalPages = users.totalPages,
            hasNext = users.hasNext,
        )
    }

    fun searchUsers(query: String, size: Int): PageResponse<UserResponse> {
        val users = userService.searchUsers(query, 0, size)
        return PageResponse(
            items = userApiMapper.toResponses(users.items),
            page = users.page,
            size = users.size,
            totalElements = users.totalElements,
            totalPages = users.totalPages,
            hasNext = users.hasNext,
        )
    }

    fun suspend(userId: UUID): UserResponse = userApiMapper.toResponse(userService.suspend(userId))

    fun activate(userId: UUID): UserResponse = userApiMapper.toResponse(userService.activate(userId))

    fun delete(userId: UUID): UserResponse = userApiMapper.toResponse(userService.delete(userId))

    fun grantValidator(userId: UUID): UserResponse = userApiMapper.toResponse(userService.grantValidator(userId))

    fun revokeValidator(userId: UUID): UserResponse = userApiMapper.toResponse(userService.revokeRole(userId, UserRole.VALIDATOR))

    fun grantOrganizer(userId: UUID): UserResponse = userApiMapper.toResponse(userService.grantOrganizer(userId))

    fun revokeOrganizer(userId: UUID): UserResponse = userApiMapper.toResponse(userService.revokeRole(userId, UserRole.ORGANIZER))
}
