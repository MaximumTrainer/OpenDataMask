package com.opendatamask.adapter.input.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opendatamask.adapter.input.rest.dto.WorkspaceConfigDto
import com.opendatamask.application.service.WorkspaceExportService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
class WorkspaceExportController(
    private val exportService: WorkspaceExportService
) {
    private val mapper = jacksonObjectMapper()

    @GetMapping("/export")
    fun export(@PathVariable workspaceId: Long): ResponseEntity<ByteArray> {
        val config = exportService.export(workspaceId)
        val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(config)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"workspace-$workspaceId-config.json\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(json)
    }

    @PostMapping("/import")
    fun import(
        @PathVariable workspaceId: Long,
        @RequestBody config: WorkspaceConfigDto
    ): ResponseEntity<Map<String, Any>> {
        exportService.import(workspaceId, config)
        return ResponseEntity.ok(mapOf("status" to "imported", "version" to config.version))
    }
}
