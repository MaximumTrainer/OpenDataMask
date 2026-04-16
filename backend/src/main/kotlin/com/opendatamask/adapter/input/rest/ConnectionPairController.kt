package com.opendatamask.adapter.input.rest

import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.application.service.ConnectionPairService
import com.opendatamask.application.service.PermissionService
import com.opendatamask.domain.model.WorkspacePermission
import com.opendatamask.domain.port.input.dto.ConnectionPairRequest
import com.opendatamask.domain.port.input.dto.ConnectionPairResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/connection-pairs")
class ConnectionPairController(
    private val connectionPairService: ConnectionPairService,
    private val permissionService: PermissionService,
    private val userRepository: UserRepository
) {

    @PostMapping
    fun createConnectionPair(
        @PathVariable workspaceId: Long,
        @Valid @RequestBody request: ConnectionPairRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ConnectionPairResponse> {
        permissionService.requirePermission(getUserId(userDetails), workspaceId, WorkspacePermission.CONFIGURE_GENERATORS)
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
        @Valid @RequestBody request: ConnectionPairRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ConnectionPairResponse> {
        permissionService.requirePermission(getUserId(userDetails), workspaceId, WorkspacePermission.CONFIGURE_GENERATORS)
        return ResponseEntity.ok(connectionPairService.updateConnectionPair(workspaceId, pairId, request))
    }

    @DeleteMapping("/{pairId}")
    fun deleteConnectionPair(
        @PathVariable workspaceId: Long,
        @PathVariable pairId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Void> {
        permissionService.requirePermission(getUserId(userDetails), workspaceId, WorkspacePermission.CONFIGURE_GENERATORS)
        connectionPairService.deleteConnectionPair(workspaceId, pairId)
        return ResponseEntity.noContent().build()
    }

    private fun getUserId(userDetails: UserDetails): Long =
        userRepository.findByUsername(userDetails.username)
            .orElseThrow { NoSuchElementException("User not found") }.id
}
