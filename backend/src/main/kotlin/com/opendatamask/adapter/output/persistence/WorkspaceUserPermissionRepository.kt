package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.WorkspaceUserPermission
import com.opendatamask.domain.port.output.WorkspaceUserPermissionPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Repository
interface WorkspaceUserPermissionRepository : JpaRepository<WorkspaceUserPermission, Long>, WorkspaceUserPermissionPort {
    override fun findById(id: Long): Optional<WorkspaceUserPermission>
    override fun findByWorkspaceUserId(workspaceUserId: Long): List<WorkspaceUserPermission>
    override fun save(permission: WorkspaceUserPermission): WorkspaceUserPermission
    override fun deleteById(id: Long)

    @Transactional
    override fun deleteByWorkspaceUserId(workspaceUserId: Long)
}
