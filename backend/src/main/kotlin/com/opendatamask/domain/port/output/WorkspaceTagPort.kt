package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.WorkspaceTag

interface WorkspaceTagPort {
    fun findByWorkspaceId(workspaceId: Long): List<WorkspaceTag>
    fun findByTag(tag: String): List<WorkspaceTag>
    fun save(tag: WorkspaceTag): WorkspaceTag
    fun deleteByWorkspaceIdAndTag(workspaceId: Long, tag: String)
    fun deleteByWorkspaceId(workspaceId: Long)
}
