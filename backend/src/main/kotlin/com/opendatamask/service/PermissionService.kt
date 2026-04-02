package com.opendatamask.service

import com.opendatamask.domain.model.WorkspacePermission
import com.opendatamask.domain.model.WorkspaceUserPermission
import com.opendatamask.adapter.output.persistence.WorkspaceUserPermissionRepository
import com.opendatamask.adapter.output.persistence.WorkspaceUserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service

@Service
class PermissionService(
    private val workspaceUserRepository: WorkspaceUserRepository,
    private val workspaceUserPermissionRepository: WorkspaceUserPermissionRepository
) {
    /**
     * Returns the effective permission set for a user in a workspace.
     * 1. Start with default permissions for their role.
     * 2. Apply explicit grants/revocations from WorkspaceUserPermission.
     */
    fun getEffectivePermissions(userId: Long, workspaceId: Long): Set<WorkspacePermission> {
        val workspaceUser = workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
            .orElseThrow { NoSuchElementException("User $userId is not a member of workspace $workspaceId") }

        val basePermissions = WorkspacePermission.DEFAULT_PERMISSIONS[workspaceUser.role] ?: emptySet()
        val overrides = workspaceUserPermissionRepository.findByWorkspaceUserId(workspaceUser.id)

        val result = basePermissions.toMutableSet()
        for (override in overrides) {
            if (override.granted) result.add(override.permission)
            else result.remove(override.permission)
        }
        return result
    }

    /**
     * Throws AccessDeniedException if the user does not hold the required permission.
     */
    fun requirePermission(userId: Long, workspaceId: Long, permission: WorkspacePermission) {
        val permissions = getEffectivePermissions(userId, workspaceId)
        if (!permissions.contains(permission)) {
            throw AccessDeniedException(
                "User $userId does not have permission $permission in workspace $workspaceId"
            )
        }
    }

    /** Returns the user's effective permissions for the workspace. */
    fun getPermissionsForUser(userId: Long, workspaceId: Long): Set<WorkspacePermission> =
        getEffectivePermissions(userId, workspaceId)
}
