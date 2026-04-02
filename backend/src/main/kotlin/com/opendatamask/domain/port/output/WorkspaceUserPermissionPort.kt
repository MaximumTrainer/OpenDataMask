package com.opendatamask.domain.port.output

import com.opendatamask.model.WorkspaceUserPermission
import java.util.Optional

interface WorkspaceUserPermissionPort {
    fun findById(id: Long): Optional<WorkspaceUserPermission>
    fun findByWorkspaceUserId(workspaceUserId: Long): List<WorkspaceUserPermission>
    fun save(permission: WorkspaceUserPermission): WorkspaceUserPermission
    fun saveAll(permissions: List<WorkspaceUserPermission>): List<WorkspaceUserPermission>
    fun deleteById(id: Long)
    fun deleteByWorkspaceUserId(workspaceUserId: Long)
}
