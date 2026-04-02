package com.opendatamask.adapter.input.rest

import com.opendatamask.adapter.input.rest.dto.*
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.adapter.output.persistence.WorkspaceTagRepository
import com.opendatamask.application.service.WorkspaceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces")
class WorkspaceController(
    private val workspaceService: WorkspaceService,
    private val userRepository: UserRepository,
    private val tagRepository: WorkspaceTagRepository
) {

    @PostMapping
    fun createWorkspace(
        @Valid @RequestBody request: WorkspaceRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<WorkspaceResponse> {
        val userId = getUserId(userDetails)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(workspaceService.createWorkspace(request, userId))
    }

    @GetMapping("/{workspaceId}")
    fun getWorkspace(@PathVariable workspaceId: Long): ResponseEntity<WorkspaceResponse> {
        return ResponseEntity.ok(workspaceService.getWorkspace(workspaceId))
    }

    @GetMapping
    fun listWorkspaces(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(required = false) tag: String?
    ): ResponseEntity<List<WorkspaceResponse>> {
        val userId = getUserId(userDetails)
        val workspaces = workspaceService.listWorkspaces(userId)
        if (tag != null) {
            val taggedWorkspaceIds = tagRepository.findByTag(tag).map { it.workspaceId }.toSet()
            return ResponseEntity.ok(workspaces.filter { it.id in taggedWorkspaceIds })
        }
        return ResponseEntity.ok(workspaces)
    }

    @PutMapping("/{workspaceId}")
    fun updateWorkspace(
        @PathVariable workspaceId: Long,
        @Valid @RequestBody request: WorkspaceRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<WorkspaceResponse> {
        val userId = getUserId(userDetails)
        return ResponseEntity.ok(workspaceService.updateWorkspace(workspaceId, request, userId))
    }

    @DeleteMapping("/{workspaceId}")
    fun deleteWorkspace(
        @PathVariable workspaceId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Void> {
        val userId = getUserId(userDetails)
        workspaceService.deleteWorkspace(workspaceId, userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{workspaceId}/users")
    fun addUserToWorkspace(
        @PathVariable workspaceId: Long,
        @RequestBody request: WorkspaceUserRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<WorkspaceUserResponse> {
        val userId = getUserId(userDetails)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(workspaceService.addUserToWorkspace(workspaceId, request, userId))
    }

    @DeleteMapping("/{workspaceId}/users/{userId}")
    fun removeUserFromWorkspace(
        @PathVariable workspaceId: Long,
        @PathVariable userId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Void> {
        val requestingUserId = getUserId(userDetails)
        workspaceService.removeUserFromWorkspace(workspaceId, userId, requestingUserId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{workspaceId}/users")
    fun getUsersInWorkspace(@PathVariable workspaceId: Long): ResponseEntity<List<WorkspaceUserResponse>> {
        return ResponseEntity.ok(workspaceService.getUsersInWorkspace(workspaceId))
    }

    private fun getUserId(userDetails: UserDetails): Long {
        return userRepository.findByUsername(userDetails.username)
            .orElseThrow { NoSuchElementException("User not found") }.id
    }
}
