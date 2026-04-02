package com.opendatamask.repository

import com.opendatamask.domain.model.DataConnection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DataConnectionRepository : JpaRepository<DataConnection, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<DataConnection>
    fun findByWorkspaceIdAndIsSource(workspaceId: Long, isSource: Boolean): List<DataConnection>
    fun deleteByWorkspaceId(workspaceId: Long)
}
