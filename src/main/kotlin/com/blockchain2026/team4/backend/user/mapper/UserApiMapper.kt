package com.blockchain2026.team4.backend.user.mapper

import com.blockchain2026.team4.backend.user.controller.response.UserResponse
import com.blockchain2026.team4.backend.user.dto.UserDto
import org.mapstruct.Mapper

@Mapper
interface UserApiMapper {
    fun toResponse(dto: UserDto): UserResponse

    fun toResponses(dtos: List<UserDto>): List<UserResponse>
}
