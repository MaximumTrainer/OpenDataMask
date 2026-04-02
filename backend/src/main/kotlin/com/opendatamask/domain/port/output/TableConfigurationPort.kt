package com.opendatamask.domain.port.output

import com.opendatamask.model.TableConfiguration
import java.util.Optional

interface TableConfigurationPort {
    fun findById(id: Long): Optional<TableConfiguration>
    fun findByWorkspaceId(workspaceId: Long): List<TableConfiguration>
    fun findByWorkspaceIdAndTableName(workspaceId: Long, tableName: String): Optional<TableConfiguration>
    fun save(config: TableConfiguration): TableConfiguration
    fun deleteById(id: Long)
    fun deleteByWorkspaceId(workspaceId: Long)
}
