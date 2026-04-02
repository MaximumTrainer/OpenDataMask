package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.WorkspaceUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface WorkspaceUserRepository : JpaRepository<WorkspaceUser, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<WorkspaceUser>
    fun findByUserId(userId: Long): List<WorkspaceUser>
    fun findByWorkspaceIdAndUserId(workspaceId: Long, userId: Long): Optional<WorkspaceUser>
    fun deleteByWorkspaceIdAndUserId(workspaceId: Long, userId: Long)
}
