package com.blockchain2026.team4.backend.common.storage

import com.blockchain2026.team4.backend.common.config.AppProperties
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Service
class LocalImageStorageService(
    private val appProperties: AppProperties,
) {
    fun store(file: MultipartFile): String {
        if (file.isEmpty) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "이미지 파일이 비어 있습니다.")
        }

        val extension = file.originalFilename
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it in ALLOWED_EXTENSIONS }
            ?: throw BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 이미지 형식입니다.")

        val directory = Path.of(appProperties.storage.imageDirectory).toAbsolutePath().normalize()
        Files.createDirectories(directory)
        val fileName = "${UUID.randomUUID()}.$extension"
        file.inputStream.use { input -> Files.copy(input, directory.resolve(fileName)) }
        return "${appProperties.storage.publicUrlPrefix.trimEnd('/')}/$fileName"
    }

    companion object {
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    }
}
