package com.opendatamask.domain.port.input

import com.opendatamask.model.WorkspacePermission

interface PermissionUseCase {
    fun getEffectivePermissions(userId: Long, workspaceId: Long): Set<WorkspacePermission>
    fun requirePermission(userId: Long, workspaceId: Long, permission: WorkspacePermission)
    fun getPermissionsForUser(userId: Long, workspaceId: Long): Set<WorkspacePermission>
}
