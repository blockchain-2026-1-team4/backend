package com.blockchain2026.team4.backend.user.mapper

import com.blockchain2026.team4.backend.user.dto.UserDto
import com.blockchain2026.team4.backend.user.entity.UserEntity
import org.mapstruct.Mapper

@Mapper
interface UserMapper {
    fun toDto(entity: UserEntity): UserDto

    fun toDtos(entities: List<UserEntity>): List<UserDto>
}
