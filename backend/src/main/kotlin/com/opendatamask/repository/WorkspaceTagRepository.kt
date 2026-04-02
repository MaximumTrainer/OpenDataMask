package com.opendatamask.repository

import com.opendatamask.domain.model.WorkspaceTag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceTagRepository : JpaRepository<WorkspaceTag, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<WorkspaceTag>
    fun findByTag(tag: String): List<WorkspaceTag>
    fun deleteByWorkspaceIdAndTag(workspaceId: Long, tag: String)
    fun deleteByWorkspaceId(workspaceId: Long)
}
