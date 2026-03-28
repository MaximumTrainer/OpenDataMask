package com.opendatamask.repository

import com.opendatamask.model.PostJobAction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostJobActionRepository : JpaRepository<PostJobAction, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<PostJobAction>
    fun findByWorkspaceIdAndEnabled(workspaceId: Long, enabled: Boolean): List<PostJobAction>
    fun deleteByWorkspaceId(workspaceId: Long)
}
