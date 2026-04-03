package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.WorkspaceUserPermission
import java.util.Optional

interface WorkspaceUserPermissionPort {
    fun findById(id: Long): Optional<WorkspaceUserPermission>
    fun findByWorkspaceUserId(workspaceUserId: Long): List<WorkspaceUserPermission>
    fun save(permission: WorkspaceUserPermission): WorkspaceUserPermission
    fun deleteById(id: Long)
    fun deleteByWorkspaceUserId(workspaceUserId: Long)
}
