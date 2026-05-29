package com.blockchain2026.team4.backend.user.service

import com.blockchain2026.team4.backend.blockchain.gateway.TrustTicketGateway
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.common.api.PageResponse
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import com.blockchain2026.team4.backend.user.dto.UserDto
import com.blockchain2026.team4.backend.user.dto.UserUpdateCommand
import com.blockchain2026.team4.backend.user.entity.UserEntity
import com.blockchain2026.team4.backend.user.entity.UserRole
import com.blockchain2026.team4.backend.user.entity.UserStatus
import com.blockchain2026.team4.backend.user.mapper.UserMapper
import com.blockchain2026.team4.backend.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val trustTicketGateway: TrustTicketGateway,
    private val blockchainTransactionService: BlockchainTransactionService,
    private val userMapper: UserMapper,
) {
    @Transactional(readOnly = true)
    fun getUser(userId: UUID): UserDto = userMapper.toDto(findEntity(userId))

    @Transactional(readOnly = true)
    fun getByWalletAddress(walletAddress: String): UserDto? =
        userRepository.findByWalletAddressIgnoreCase(walletAddress.normalizeWallet())?.let(userMapper::toDto)

    @Transactional
    fun getOrCreateWalletUser(walletAddress: String): UserDto {
        val normalizedWallet = walletAddress.normalizeWallet()
        val entity = userRepository.findByWalletAddressIgnoreCase(normalizedWallet)
            ?: userRepository.save(UserEntity(walletAddress = normalizedWallet, roles = mutableSetOf(UserRole.USER)))
        ensureActive(entity)
        return userMapper.toDto(entity)
    }

    @Transactional
    fun createEmailUser(email: String, passwordHash: String, displayName: String?): UserDto {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw BusinessException(ErrorCode.CONFLICT, "이미 가입된 이메일입니다.")
        }
        val entity = UserEntity(
            email = email.lowercase(),
            passwordHash = passwordHash,
            displayName = displayName,
            roles = mutableSetOf(UserRole.USER),
        )
        return userMapper.toDto(userRepository.save(entity))
    }

    @Transactional(readOnly = true)
    fun findEmailLoginUser(email: String): Pair<UserDto, String> {
        val entity = userRepository.findByEmailIgnoreCase(email)
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.")
        ensureActive(entity)
        return userMapper.toDto(entity) to (entity.passwordHash ?: "")
    }

    @Transactional
    fun updateMe(userId: UUID, command: UserUpdateCommand): UserDto {
        val entity = findEntity(userId)
        entity.displayName = command.displayName
        return userMapper.toDto(entity)
    }

    @Transactional(readOnly = true)
    fun listUsers(page: Int, size: Int, status: UserStatus?): PageResponse<UserDto> {
        val pageable = PageRequest.of(page, size)
        val users = status?.let { userRepository.findAllByStatus(it, pageable) } ?: userRepository.findAll(pageable)
        return PageResponse(
            items = users.content.map(userMapper::toDto),
            page = users.number,
            size = users.size,
            totalElements = users.totalElements,
            totalPages = users.totalPages,
            hasNext = users.hasNext(),
        )
    }

    @Transactional(readOnly = true)
    fun searchUsers(query: String, page: Int, size: Int): PageResponse<UserDto> {
        val pageable = PageRequest.of(page, size)
        val users = userRepository.searchByEmailOrDisplayName(query.trim(), pageable)
        return PageResponse(
            items = users.content.map(userMapper::toDto),
            page = users.number,
            size = users.size,
            totalElements = users.totalElements,
            totalPages = users.totalPages,
            hasNext = users.hasNext(),
        )
    }

    @Transactional
    fun grantRole(userId: UUID, role: UserRole): UserDto {
        val entity = findEntity(userId)
        entity.roles.add(role)
        return userMapper.toDto(entity)
    }

    @Transactional
    fun revokeRole(userId: UUID, role: UserRole): UserDto {
        val entity = findEntity(userId)
        if (role == UserRole.USER) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "USER 기본 권한은 회수할 수 없습니다.")
        }
        entity.roles.remove(role)
        return userMapper.toDto(entity)
    }

    @Transactional
    fun grantOrganizer(userId: UUID): UserDto {
        val entity = findEntity(userId)
        entity.roles.add(UserRole.ORGANIZER)
        entity.walletAddress?.let {
            val submission = trustTicketGateway.addOrganizer(it)
            blockchainTransactionService.record(submission)
        }
        return userMapper.toDto(entity)
    }

    @Transactional
    fun suspend(userId: UUID): UserDto {
        val entity = findEntity(userId)
        entity.status = UserStatus.SUSPENDED
        return userMapper.toDto(entity)
    }

    @Transactional
    fun activate(userId: UUID): UserDto {
        val entity = findEntity(userId)
        entity.status = UserStatus.ACTIVE
        return userMapper.toDto(entity)
    }

    @Transactional
    fun delete(userId: UUID): UserDto {
        val entity = findEntity(userId)
        entity.status = UserStatus.DELETED
        return userMapper.toDto(entity)
    }

    @Transactional
    fun grantValidator(userId: UUID): UserDto {
        val entity = findEntity(userId)
        entity.roles.add(UserRole.VALIDATOR)
        entity.walletAddress?.let {
            val submission = trustTicketGateway.addValidator(it)
            blockchainTransactionService.record(submission)
        }
        return userMapper.toDto(entity)
    }

    @Transactional(readOnly = true)
    fun requireRole(userId: UUID, role: UserRole): UserDto {
        val entity = findEntity(userId)
        ensureActive(entity)
        if (!entity.roles.contains(role)) {
            throw BusinessException(ErrorCode.FORBIDDEN, "${role.name} 권한이 필요합니다.")
        }
        return userMapper.toDto(entity)
    }

    @Transactional(readOnly = true)
    fun findEntity(userId: UUID): UserEntity =
        userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다.") }

    private fun ensureActive(entity: UserEntity) {
        if (entity.status != UserStatus.ACTIVE) {
            throw BusinessException(ErrorCode.FORBIDDEN, "사용할 수 없는 계정입니다.")
        }
    }

    private fun String.normalizeWallet(): String = trim().lowercase()
}
