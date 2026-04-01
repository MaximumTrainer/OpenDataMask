package com.opendatamask.controller

import com.opendatamask.dto.ForeignKeyRelationshipRequest
import com.opendatamask.dto.ForeignKeyRelationshipResponse
import com.opendatamask.dto.toResponse
import com.opendatamask.service.ForeignKeyDiscoveryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/foreign-keys")
class ForeignKeyRelationshipController(
    private val foreignKeyDiscoveryService: ForeignKeyDiscoveryService
) {

    @GetMapping
    fun listForeignKeys(
        @PathVariable workspaceId: Long
    ): ResponseEntity<List<ForeignKeyRelationshipResponse>> {
        val fks = foreignKeyDiscoveryService.getVirtualForeignKeys(workspaceId)
        return ResponseEntity.ok(fks.map { it.toResponse() })
    }

    @PostMapping
    fun createVirtualForeignKey(
        @PathVariable workspaceId: Long,
        @Valid @RequestBody request: ForeignKeyRelationshipRequest
    ): ResponseEntity<ForeignKeyRelationshipResponse> {
        val fk = foreignKeyDiscoveryService.createVirtualForeignKey(
            workspaceId = workspaceId,
            fromTable = request.fromTable,
            fromColumn = request.fromColumn,
            toTable = request.toTable,
            toColumn = request.toColumn
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(fk.toResponse())
    }

    @DeleteMapping("/{fkId}")
    fun deleteForeignKey(
        @PathVariable workspaceId: Long,
        @PathVariable fkId: Long
    ): ResponseEntity<Void> {
        foreignKeyDiscoveryService.deleteForeignKey(workspaceId, fkId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/discover")
    fun discoverForeignKeys(
        @PathVariable workspaceId: Long
    ): ResponseEntity<List<ForeignKeyRelationshipResponse>> {
        val fks = foreignKeyDiscoveryService.discoverForeignKeys(workspaceId)
        return ResponseEntity.ok(fks.map { it.toResponse() })
    }
}
