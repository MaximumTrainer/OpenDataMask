package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.WorkspaceUser
import java.util.Optional

interface WorkspaceUserPort {
    fun findById(id: Long): Optional<WorkspaceUser>
    fun findByWorkspaceId(workspaceId: Long): List<WorkspaceUser>
    fun findByUserId(userId: Long): List<WorkspaceUser>
    fun findByWorkspaceIdAndUserId(workspaceId: Long, userId: Long): Optional<WorkspaceUser>
    fun save(workspaceUser: WorkspaceUser): WorkspaceUser
    fun delete(workspaceUser: WorkspaceUser)
    fun deleteByWorkspaceIdAndUserId(workspaceId: Long, userId: Long)
}
