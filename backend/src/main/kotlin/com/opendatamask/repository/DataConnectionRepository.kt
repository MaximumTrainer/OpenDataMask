package com.opendatamask.repository

import com.opendatamask.model.DataConnection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DataConnectionRepository : JpaRepository<DataConnection, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<DataConnection>
    fun deleteByWorkspaceId(workspaceId: Long)
}
