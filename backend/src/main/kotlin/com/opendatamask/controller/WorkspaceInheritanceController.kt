package com.opendatamask.controller

import com.opendatamask.dto.*
import com.opendatamask.domain.model.InheritedConfig
import com.opendatamask.domain.model.Workspace
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.service.WorkspaceInheritanceService
import com.opendatamask.service.WorkspaceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces")
class WorkspaceInheritanceController(
    private val workspaceService: WorkspaceService,
    private val workspaceInheritanceService: WorkspaceInheritanceService,
    private val userRepository: UserRepository
) {

    /** Lists all direct child workspaces of the given parent workspace. */
    @GetMapping("/{id}/children")
    fun listChildren(@PathVariable id: Long): ResponseEntity<List<WorkspaceResponse>> {
        val children = workspaceInheritanceService.listChildWorkspaces(id)
        return ResponseEntity.ok(children.map { it.toResponse() })
    }

    /** Creates a child workspace under the given parent and optionally inherits its config. */
    @PostMapping("/{id}/children")
    fun createChild(
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateChildWorkspaceRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<WorkspaceResponse> {
        val ownerId = getUserId(userDetails)
        val workspaceRequest = WorkspaceRequest(
            name = request.name,
            description = request.description,
            parentWorkspaceId = id,
            inheritanceEnabled = request.inheritanceEnabled
        )
        val response = workspaceService.createWorkspace(workspaceRequest, ownerId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /** Triggers inheritance from a specific parent workspace into this child workspace. */
    @PostMapping("/{id}/inherit/{parentId}")
    fun inheritFromParent(
        @PathVariable id: Long,
        @PathVariable parentId: Long
    ): ResponseEntity<Void> {
        workspaceInheritanceService.inheritFromParent(id, parentId)
        return ResponseEntity.ok().build()
    }

    /** Re-syncs the child workspace with its configured parent workspace. */
    @PostMapping("/{id}/sync-parent")
    fun syncWithParent(@PathVariable id: Long): ResponseEntity<Void> {
        workspaceInheritanceService.syncWithParent(id)
        return ResponseEntity.ok().build()
    }

    /** Lists all configs in the child workspace that are still inherited (not overridden). */
    @GetMapping("/{id}/inherited-configs")
    fun listInheritedConfigs(@PathVariable id: Long): ResponseEntity<List<InheritedConfigResponse>> {
        val configs = workspaceInheritanceService.listInheritedConfigs(id)
        return ResponseEntity.ok(configs.map { it.toResponse() })
    }

    /** Marks an inherited config as locally overridden (removes inheritance tracking). */
    @PostMapping("/{id}/inherited-configs/{inheritedConfigId}/override")
    fun markAsOverridden(
        @PathVariable id: Long,
        @PathVariable inheritedConfigId: Long
    ): ResponseEntity<Void> {
        workspaceInheritanceService.markAsOverridden(inheritedConfigId)
        return ResponseEntity.ok().build()
    }

    private fun getUserId(userDetails: UserDetails): Long =
        userRepository.findByUsername(userDetails.username)
            .orElseThrow { NoSuchElementException("User not found") }.id

    private fun Workspace.toResponse() = WorkspaceResponse(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        parentWorkspaceId = parentWorkspaceId,
        inheritanceEnabled = inheritanceEnabled
    )

    private fun InheritedConfig.toResponse() = InheritedConfigResponse(
        id = id,
        childWorkspaceId = childWorkspaceId,
        parentWorkspaceId = parentWorkspaceId,
        configType = configType,
        tableName = tableName,
        columnName = columnName,
        inheritedEntityId = inheritedEntityId,
        inheritedAt = inheritedAt
    )
}
