package com.opendatamask.repository

import com.opendatamask.model.WorkspaceUserPermission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface WorkspaceUserPermissionRepository : JpaRepository<WorkspaceUserPermission, Long> {
    fun findByWorkspaceUserId(workspaceUserId: Long): List<WorkspaceUserPermission>

    @Transactional
    fun deleteByWorkspaceUserId(workspaceUserId: Long)
}
