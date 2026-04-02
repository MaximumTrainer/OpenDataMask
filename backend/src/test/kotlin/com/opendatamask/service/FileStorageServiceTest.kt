package com.opendatamask.service

import com.opendatamask.config.EncryptionService
import com.opendatamask.domain.model.StoredFile
import com.opendatamask.adapter.output.persistence.StoredFileRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import java.util.Optional

class FileStorageServiceTest {

    private val encryptionService = mock<EncryptionService>()
    private val storedFileRepository = mock<StoredFileRepository>()
    private val service = FileStorageService(encryptionService, storedFileRepository)

    @Test
    fun `storeFile encrypts content and saves to repository`() {
        val content = "id,name\n1,Alice".toByteArray()
        val encrypted = "encrypted-base64-content"
        whenever(encryptionService.encrypt(any())).thenReturn(encrypted)
        val savedFile = StoredFile(id = 1L, workspaceId = 1L, filename = "test.csv", contentType = "text/csv", isSource = true, encryptedContent = encrypted)
        whenever(storedFileRepository.save(any<StoredFile>())).thenReturn(savedFile)

        val result = service.storeFile(workspaceId = 1L, filename = "test.csv", contentType = "text/csv", content = content, isSource = true)

        verify(encryptionService).encrypt(any())
        verify(storedFileRepository).save(any<StoredFile>())
        assertEquals(1L, result.id)
    }

    @Test
    fun `retrieveFile decrypts content from repository`() {
        val encrypted = "encrypted-base64-content"
        val decrypted = java.util.Base64.getEncoder().encodeToString("id,name\n1,Alice".toByteArray())
        val storedFile = StoredFile(id = 1L, workspaceId = 1L, filename = "test.csv", contentType = "text/csv", isSource = true, encryptedContent = encrypted)
        whenever(storedFileRepository.findById(1L)).thenReturn(Optional.of(storedFile))
        whenever(encryptionService.decrypt(encrypted)).thenReturn(decrypted)

        val result = service.retrieveFile(1L)

        assertEquals("id,name\n1,Alice".toByteArray().size, result.content.size)
        assertEquals("test.csv", result.filename)
    }

    @Test
    fun `retrieveFile throws when file not found`() {
        whenever(storedFileRepository.findById(999L)).thenReturn(Optional.empty())
        assertThrows(NoSuchElementException::class.java) { service.retrieveFile(999L) }
    }
}
