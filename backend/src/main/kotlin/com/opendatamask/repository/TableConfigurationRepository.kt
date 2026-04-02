package com.opendatamask.repository

import com.opendatamask.domain.model.TableConfiguration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TableConfigurationRepository : JpaRepository<TableConfiguration, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<TableConfiguration>
    fun findByWorkspaceIdAndTableName(workspaceId: Long, tableName: String): Optional<TableConfiguration>
    fun deleteByWorkspaceId(workspaceId: Long)
}
