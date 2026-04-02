package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.port.input.dto.ConnectionTestResult
import com.opendatamask.domain.port.input.dto.DataConnectionRequest
import com.opendatamask.domain.port.input.dto.DataConnectionResponse
import com.opendatamask.application.service.DataConnectionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/connections")
class DataConnectionController(
    private val dataConnectionService: DataConnectionService
) {

    @PostMapping
    fun createConnection(
        @PathVariable workspaceId: Long,
        @Valid @RequestBody request: DataConnectionRequest
    ): ResponseEntity<DataConnectionResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(dataConnectionService.createConnection(workspaceId, request))
    }

    @GetMapping("/{connectionId}")
    fun getConnection(
        @PathVariable workspaceId: Long,
        @PathVariable connectionId: Long
    ): ResponseEntity<DataConnectionResponse> {
        return ResponseEntity.ok(dataConnectionService.getConnection(workspaceId, connectionId))
    }

    @GetMapping
    fun listConnections(@PathVariable workspaceId: Long): ResponseEntity<List<DataConnectionResponse>> {
        return ResponseEntity.ok(dataConnectionService.listConnections(workspaceId))
    }

    @PutMapping("/{connectionId}")
    fun updateConnection(
        @PathVariable workspaceId: Long,
        @PathVariable connectionId: Long,
        @Valid @RequestBody request: DataConnectionRequest
    ): ResponseEntity<DataConnectionResponse> {
        return ResponseEntity.ok(dataConnectionService.updateConnection(workspaceId, connectionId, request))
    }

    @DeleteMapping("/{connectionId}")
    fun deleteConnection(
        @PathVariable workspaceId: Long,
        @PathVariable connectionId: Long
    ): ResponseEntity<Void> {
        dataConnectionService.deleteConnection(workspaceId, connectionId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{connectionId}/test")
    fun testConnection(
        @PathVariable workspaceId: Long,
        @PathVariable connectionId: Long
    ): ResponseEntity<ConnectionTestResult> {
        return ResponseEntity.ok(dataConnectionService.testConnection(workspaceId, connectionId))
    }
}

