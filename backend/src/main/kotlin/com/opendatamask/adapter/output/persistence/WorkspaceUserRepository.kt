package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.WorkspaceUser
import com.opendatamask.domain.port.output.WorkspaceUserPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface WorkspaceUserRepository : JpaRepository<WorkspaceUser, Long>, WorkspaceUserPort {
    override fun findById(id: Long): Optional<WorkspaceUser>
    override fun findByWorkspaceId(workspaceId: Long): List<WorkspaceUser>
    override fun findByUserId(userId: Long): List<WorkspaceUser>
    override fun findByWorkspaceIdAndUserId(workspaceId: Long, userId: Long): Optional<WorkspaceUser>
    override fun save(workspaceUser: WorkspaceUser): WorkspaceUser
    override fun deleteByWorkspaceIdAndUserId(workspaceId: Long, userId: Long)
}
