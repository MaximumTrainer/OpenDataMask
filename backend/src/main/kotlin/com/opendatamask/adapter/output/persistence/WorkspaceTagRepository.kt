package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.WorkspaceTag
import com.opendatamask.domain.port.output.WorkspaceTagPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceTagRepository : JpaRepository<WorkspaceTag, Long>, WorkspaceTagPort {
    override fun findByWorkspaceId(workspaceId: Long): List<WorkspaceTag>
    override fun findByTag(tag: String): List<WorkspaceTag>
    override fun deleteByWorkspaceIdAndTag(workspaceId: Long, tag: String)
    override fun deleteByWorkspaceId(workspaceId: Long)
}
