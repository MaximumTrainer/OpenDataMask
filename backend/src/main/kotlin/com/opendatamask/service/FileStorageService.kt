package com.opendatamask.service

import com.opendatamask.config.EncryptionService
import com.opendatamask.domain.model.StoredFile
import com.opendatamask.repository.StoredFileRepository
import org.springframework.stereotype.Service
import java.util.Base64

data class RetrievedFile(val filename: String, val contentType: String, val content: ByteArray, val isSource: Boolean)

@Service
class FileStorageService(
    private val encryptionService: EncryptionService,
    private val storedFileRepository: StoredFileRepository
) {
    fun storeFile(workspaceId: Long, filename: String, contentType: String, content: ByteArray, isSource: Boolean): StoredFile {
        val base64Content = Base64.getEncoder().encodeToString(content)
        val encrypted = encryptionService.encrypt(base64Content)
        val storedFile = StoredFile(
            workspaceId = workspaceId,
            filename = filename,
            contentType = contentType,
            isSource = isSource,
            encryptedContent = encrypted
        )
        return storedFileRepository.save(storedFile)
    }

    fun retrieveFile(fileId: Long): RetrievedFile {
        val storedFile = storedFileRepository.findById(fileId)
            .orElseThrow { NoSuchElementException("File not found: $fileId") }
        val decrypted = encryptionService.decrypt(storedFile.encryptedContent)
        val content = Base64.getDecoder().decode(decrypted)
        return RetrievedFile(
            filename = storedFile.filename,
            contentType = storedFile.contentType,
            content = content,
            isSource = storedFile.isSource
        )
    }

    fun listFiles(workspaceId: Long): List<StoredFile> = storedFileRepository.findByWorkspaceId(workspaceId)

    fun listSourceFiles(workspaceId: Long): List<StoredFile> = storedFileRepository.findByWorkspaceIdAndIsSource(workspaceId, true)

    fun deleteFile(fileId: Long) = storedFileRepository.deleteById(fileId)
}
