package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.SubsetTableConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SubsetTableConfigRepository : JpaRepository<SubsetTableConfig, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<SubsetTableConfig>
    fun findByWorkspaceIdAndTableName(workspaceId: Long, tableName: String): SubsetTableConfig?
    fun deleteByWorkspaceId(workspaceId: Long)
}
