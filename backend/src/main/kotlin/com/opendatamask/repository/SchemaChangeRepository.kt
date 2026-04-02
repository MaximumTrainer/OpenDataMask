package com.opendatamask.repository

import com.opendatamask.domain.model.SchemaChange
import com.opendatamask.domain.model.SchemaChangeStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SchemaChangeRepository : JpaRepository<SchemaChange, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<SchemaChange>
    fun findByWorkspaceIdAndStatus(workspaceId: Long, status: SchemaChangeStatus): List<SchemaChange>
    fun deleteByWorkspaceId(workspaceId: Long)
}
