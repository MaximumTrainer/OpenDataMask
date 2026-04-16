package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.CustomDataMapping
import java.util.Optional

interface CustomDataMappingPort {
    fun findById(id: Long): Optional<CustomDataMapping>
    fun findByWorkspaceId(workspaceId: Long): List<CustomDataMapping>
    fun findByWorkspaceIdAndConnectionIdAndTableName(
        workspaceId: Long,
        connectionId: Long,
        tableName: String
    ): List<CustomDataMapping>
    fun save(mapping: CustomDataMapping): CustomDataMapping
    fun deleteById(id: Long)
    fun deleteByWorkspaceIdAndConnectionIdAndTableName(
        workspaceId: Long,
        connectionId: Long,
        tableName: String
    )
}
