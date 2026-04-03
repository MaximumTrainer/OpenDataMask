package com.opendatamask.application.service

import com.opendatamask.domain.port.input.FileStorageUseCase
import com.opendatamask.domain.port.input.RetrievedFile

import com.opendatamask.domain.port.output.EncryptionPort
import com.opendatamask.domain.model.StoredFile
import com.opendatamask.domain.port.output.StoredFilePort
import org.springframework.stereotype.Service
import java.util.Base64


@Service
class FileStorageService(
    private val encryptionPort: EncryptionPort,
    private val storedFileRepository: StoredFilePort
) : FileStorageUseCase {
    override fun storeFile(workspaceId: Long, filename: String, contentType: String, content: ByteArray, isSource: Boolean): StoredFile {
        val base64Content = Base64.getEncoder().encodeToString(content)
        val encrypted = encryptionPort.encrypt(base64Content)
        val storedFile = StoredFile(
            workspaceId = workspaceId,
            filename = filename,
            contentType = contentType,
            isSource = isSource,
            encryptedContent = encrypted
        )
        return storedFileRepository.save(storedFile)
    }

    override fun retrieveFile(fileId: Long): RetrievedFile {
        val storedFile = storedFileRepository.findById(fileId)
            .orElseThrow { NoSuchElementException("File not found: $fileId") }
        val decrypted = encryptionPort.decrypt(storedFile.encryptedContent)
        val content = Base64.getDecoder().decode(decrypted)
        return RetrievedFile(
            filename = storedFile.filename,
            contentType = storedFile.contentType,
            content = content,
            isSource = storedFile.isSource
        )
    }

    override fun listFiles(workspaceId: Long): List<StoredFile> = storedFileRepository.findByWorkspaceId(workspaceId)

    override fun listSourceFiles(workspaceId: Long): List<StoredFile> = storedFileRepository.findByWorkspaceIdAndIsSource(workspaceId, true)

    override fun deleteFile(fileId: Long) = storedFileRepository.deleteById(fileId)
}
