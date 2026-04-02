package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.SchemaChange
import com.opendatamask.domain.model.SchemaChangeHandling
import com.opendatamask.domain.model.SchemaChangeType
import com.opendatamask.domain.model.SchemaChangeStatus
import com.opendatamask.adapter.output.persistence.WorkspaceRepository
import com.opendatamask.application.service.SchemaChangeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/schema-changes")
class SchemaChangeController(
    private val schemaChangeService: SchemaChangeService,
    private val workspaceRepository: WorkspaceRepository
) {
    data class SchemaChangesResponse(
        val exposing: List<SchemaChange>,
        val notifications: List<SchemaChange>
    )

    @GetMapping
    fun getChanges(@PathVariable workspaceId: Long): ResponseEntity<SchemaChangesResponse> {
        val changes = schemaChangeService.getUnresolvedChanges(workspaceId)
        val exposingTypes = setOf(SchemaChangeType.NEW_COLUMN, SchemaChangeType.TYPE_CHANGED, SchemaChangeType.NULLABILITY_CHANGED)
        val exposing = changes.filter { it.changeType in exposingTypes }
        val notifications = changes.filter { it.changeType !in exposingTypes }
        return ResponseEntity.ok(SchemaChangesResponse(exposing, notifications))
    }

    @PostMapping("/detect")
    fun runDetection(@PathVariable workspaceId: Long): ResponseEntity<List<SchemaChange>> {
        val changes = schemaChangeService.detectChanges(workspaceId)
        return ResponseEntity.ok(changes)
    }

    @PostMapping("/{changeId}/resolve")
    fun resolve(@PathVariable workspaceId: Long, @PathVariable changeId: Long): ResponseEntity<Void> {
        schemaChangeService.resolveChange(changeId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{changeId}/dismiss")
    fun dismiss(@PathVariable workspaceId: Long, @PathVariable changeId: Long): ResponseEntity<Void> {
        schemaChangeService.dismissChange(changeId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/resolve-all")
    fun resolveAll(@PathVariable workspaceId: Long): ResponseEntity<Void> {
        schemaChangeService.resolveAll(workspaceId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/dismiss-all")
    fun dismissAll(@PathVariable workspaceId: Long): ResponseEntity<Void> {
        schemaChangeService.dismissAll(workspaceId)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/settings")
    fun updateSettings(
        @PathVariable workspaceId: Long,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<Void> {
        val workspace = workspaceRepository.findById(workspaceId).orElseThrow()
        workspace.schemaChangeHandling = SchemaChangeHandling.valueOf(
            body["schemaChangeHandling"] ?: SchemaChangeHandling.BLOCK_EXPOSING.name
        )
        workspaceRepository.save(workspace)
        return ResponseEntity.ok().build()
    }
}


