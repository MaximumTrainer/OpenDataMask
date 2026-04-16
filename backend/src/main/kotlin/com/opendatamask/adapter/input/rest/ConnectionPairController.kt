package com.opendatamask.adapter.input.rest

import com.opendatamask.application.service.ConnectionPairService
import com.opendatamask.domain.port.input.dto.ConnectionPairRequest
import com.opendatamask.domain.port.input.dto.ConnectionPairResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/connection-pairs")
class ConnectionPairController(
    private val connectionPairService: ConnectionPairService
) {

    @PostMapping
    fun createConnectionPair(
        @PathVariable workspaceId: Long,
        @Valid @RequestBody request: ConnectionPairRequest
    ): ResponseEntity<ConnectionPairResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(connectionPairService.createConnectionPair(workspaceId, request))
    }

    @GetMapping("/{pairId}")
    fun getConnectionPair(
        @PathVariable workspaceId: Long,
        @PathVariable pairId: Long
    ): ResponseEntity<ConnectionPairResponse> {
        return ResponseEntity.ok(connectionPairService.getConnectionPair(workspaceId, pairId))
    }

    @GetMapping
    fun listConnectionPairs(@PathVariable workspaceId: Long): ResponseEntity<List<ConnectionPairResponse>> {
        return ResponseEntity.ok(connectionPairService.listConnectionPairs(workspaceId))
    }

    @PutMapping("/{pairId}")
    fun updateConnectionPair(
        @PathVariable workspaceId: Long,
        @PathVariable pairId: Long,
        @Valid @RequestBody request: ConnectionPairRequest
    ): ResponseEntity<ConnectionPairResponse> {
        return ResponseEntity.ok(connectionPairService.updateConnectionPair(workspaceId, pairId, request))
    }

    @DeleteMapping("/{pairId}")
    fun deleteConnectionPair(
        @PathVariable workspaceId: Long,
        @PathVariable pairId: Long
    ): ResponseEntity<Void> {
        connectionPairService.deleteConnectionPair(workspaceId, pairId)
        return ResponseEntity.noContent().build()
    }
}
