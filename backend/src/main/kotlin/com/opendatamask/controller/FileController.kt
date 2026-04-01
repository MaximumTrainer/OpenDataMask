package com.opendatamask.controller

import com.opendatamask.service.FileStorageService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/files")
class FileController(
    private val fileStorageService: FileStorageService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @PathVariable workspaceId: Long,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("isSource", defaultValue = "true") isSource: Boolean
    ): ResponseEntity<Map<String, Any>> {
        val stored = fileStorageService.storeFile(
            workspaceId = workspaceId,
            filename = file.originalFilename ?: file.name,
            contentType = file.contentType ?: "application/octet-stream",
            content = file.bytes,
            isSource = isSource
        )
        return ResponseEntity.ok(mapOf(
            "id" to stored.id,
            "filename" to stored.filename,
            "contentType" to stored.contentType,
            "isSource" to stored.isSource,
            "createdAt" to stored.createdAt.toString()
        ))
    }

    @GetMapping
    fun listFiles(@PathVariable workspaceId: Long): ResponseEntity<List<Map<String, Any>>> {
        val files = fileStorageService.listFiles(workspaceId).map { f ->
            mapOf(
                "id" to f.id,
                "filename" to f.filename,
                "contentType" to f.contentType,
                "isSource" to f.isSource,
                "createdAt" to f.createdAt.toString()
            )
        }
        return ResponseEntity.ok(files)
    }

    @GetMapping("/{fileId}")
    fun downloadFile(
        @PathVariable workspaceId: Long,
        @PathVariable fileId: Long
    ): ResponseEntity<ByteArray> {
        val retrieved = fileStorageService.retrieveFile(fileId)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${retrieved.filename}\"")
            .contentType(MediaType.parseMediaType(retrieved.contentType))
            .body(retrieved.content)
    }

    @DeleteMapping("/{fileId}")
    fun deleteFile(
        @PathVariable workspaceId: Long,
        @PathVariable fileId: Long
    ): ResponseEntity<Void> {
        fileStorageService.deleteFile(fileId)
        return ResponseEntity.noContent().build()
    }
}
