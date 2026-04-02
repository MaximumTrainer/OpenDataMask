package com.opendatamask.domain.port.output

import com.opendatamask.model.InheritedConfig
import java.util.Optional

interface InheritedConfigPort {
    fun findById(id: Long): Optional<InheritedConfig>
    fun findByChildWorkspaceId(childWorkspaceId: Long): List<InheritedConfig>
    fun findByChildWorkspaceIdAndTableName(childWorkspaceId: Long, tableName: String): List<InheritedConfig>
    fun save(config: InheritedConfig): InheritedConfig
    fun saveAll(configs: List<InheritedConfig>): List<InheritedConfig>
    fun deleteById(id: Long)
    fun deleteByChildWorkspaceId(childWorkspaceId: Long)
}
