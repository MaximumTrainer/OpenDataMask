package com.opendatamask.adapter.input.rest

import com.opendatamask.adapter.input.rest.dto.UpdatePermissionsRequest
import com.opendatamask.domain.model.WorkspacePermission
import com.opendatamask.domain.model.WorkspaceRole
import com.opendatamask.domain.model.WorkspaceUserPermission
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.adapter.output.persistence.WorkspaceUserPermissionRepository
import com.opendatamask.adapter.output.persistence.WorkspaceUserRepository
import com.opendatamask.application.service.PermissionService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{id}/users/{userId}/permissions")
class WorkspacePermissionController(
    private val permissionService: PermissionService,
    private val workspaceUserRepository: WorkspaceUserRepository,
    private val workspaceUserPermissionRepository: WorkspaceUserPermissionRepository,
    private val userRepository: UserRepository
) {

    @GetMapping
    fun getPermissions(
        @PathVariable id: Long,
        @PathVariable userId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Set<WorkspacePermission>> {
        requireAdmin(id, getCallerId(userDetails))
        return ResponseEntity.ok(permissionService.getEffectivePermissions(userId, id))
    }

    @PutMapping
    fun updatePermissions(
        @PathVariable id: Long,
        @PathVariable userId: Long,
        @RequestBody request: UpdatePermissionsRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Set<WorkspacePermission>> {
        requireAdmin(id, getCallerId(userDetails))

        val workspaceUser = workspaceUserRepository.findByWorkspaceIdAndUserId(id, userId)
            .orElseThrow { NoSuchElementException("User $userId is not a member of workspace $id") }

        val existingOverrides = workspaceUserPermissionRepository.findByWorkspaceUserId(workspaceUser.id)

        for (permName in request.grants) {
            val perm = WorkspacePermission.valueOf(permName)
            existingOverrides.find { it.permission == perm }?.let {
                workspaceUserPermissionRepository.delete(it)
            }
            workspaceUserPermissionRepository.save(
                WorkspaceUserPermission(workspaceUserId = workspaceUser.id, permission = perm, granted = true)
            )
        }

        for (permName in request.revocations) {
            val perm = WorkspacePermission.valueOf(permName)
            existingOverrides.find { it.permission == perm }?.let {
                workspaceUserPermissionRepository.delete(it)
            }
            workspaceUserPermissionRepository.save(
                WorkspaceUserPermission(workspaceUserId = workspaceUser.id, permission = perm, granted = false)
            )
        }

        return ResponseEntity.ok(permissionService.getEffectivePermissions(userId, id))
    }

    @DeleteMapping
    fun deletePermissions(
        @PathVariable id: Long,
        @PathVariable userId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Void> {
        requireAdmin(id, getCallerId(userDetails))

        val workspaceUser = workspaceUserRepository.findByWorkspaceIdAndUserId(id, userId)
            .orElseThrow { NoSuchElementException("User $userId is not a member of workspace $id") }

        workspaceUserPermissionRepository.deleteByWorkspaceUserId(workspaceUser.id)
        return ResponseEntity.noContent().build()
    }

    private fun getCallerId(userDetails: UserDetails): Long =
        userRepository.findByUsername(userDetails.username)
            .orElseThrow { NoSuchElementException("User not found") }.id

    private fun requireAdmin(workspaceId: Long, callerId: Long) {
        val wu = workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, callerId)
            .orElseThrow { AccessDeniedException("Access denied") }
        if (wu.role != WorkspaceRole.ADMIN) {
            throw AccessDeniedException("Admin role required for workspace $workspaceId")
        }
    }
}
