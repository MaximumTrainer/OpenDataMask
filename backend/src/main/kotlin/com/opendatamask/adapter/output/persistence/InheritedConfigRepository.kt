package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.InheritedConfig
import com.opendatamask.domain.port.output.InheritedConfigPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InheritedConfigRepository : JpaRepository<InheritedConfig, Long>, InheritedConfigPort {
    override fun findById(id: Long): Optional<InheritedConfig>
    override fun findByChildWorkspaceId(childWorkspaceId: Long): List<InheritedConfig>
    override fun findByChildWorkspaceIdAndTableName(childWorkspaceId: Long, tableName: String): List<InheritedConfig>
    override fun existsById(id: Long): Boolean
    override fun save(config: InheritedConfig): InheritedConfig
    override fun deleteById(id: Long)
    override fun deleteByChildWorkspaceId(childWorkspaceId: Long)
}
