package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.SubsetTableConfig
import java.util.Optional

interface SubsetTableConfigPort {
    fun findById(id: Long): Optional<SubsetTableConfig>
    fun findByWorkspaceId(workspaceId: Long): List<SubsetTableConfig>
    fun findByWorkspaceIdAndTableName(workspaceId: Long, tableName: String): SubsetTableConfig?
    fun save(config: SubsetTableConfig): SubsetTableConfig
    fun deleteById(id: Long)
    fun deleteByWorkspaceId(workspaceId: Long)
}
