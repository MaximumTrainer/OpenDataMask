package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.PostJobAction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostJobActionRepository : JpaRepository<PostJobAction, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<PostJobAction>
    fun findByWorkspaceIdAndEnabled(workspaceId: Long, enabled: Boolean): List<PostJobAction>
    fun deleteByWorkspaceId(workspaceId: Long)
}
