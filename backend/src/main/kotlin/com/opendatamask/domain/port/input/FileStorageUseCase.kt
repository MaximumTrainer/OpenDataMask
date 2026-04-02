package com.opendatamask.domain.port.input

import com.opendatamask.model.StoredFile

data class RetrievedFile(val filename: String, val contentType: String, val content: ByteArray, val isSource: Boolean)

interface FileStorageUseCase {
    fun storeFile(workspaceId: Long, filename: String, contentType: String, content: ByteArray, isSource: Boolean): StoredFile
    fun retrieveFile(fileId: Long): RetrievedFile
    fun listFiles(workspaceId: Long): List<StoredFile>
    fun listSourceFiles(workspaceId: Long): List<StoredFile>
    fun deleteFile(fileId: Long)
}
