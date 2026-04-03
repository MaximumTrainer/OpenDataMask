package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.TableConfiguration
import com.opendatamask.domain.port.output.TableConfigurationPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TableConfigurationRepository : JpaRepository<TableConfiguration, Long>, TableConfigurationPort {
    override fun findById(id: Long): Optional<TableConfiguration>
    override fun findByWorkspaceId(workspaceId: Long): List<TableConfiguration>
    override fun findByWorkspaceIdAndTableName(workspaceId: Long, tableName: String): Optional<TableConfiguration>
    override fun save(config: TableConfiguration): TableConfiguration
    override fun deleteById(id: Long)
    override fun deleteByWorkspaceId(workspaceId: Long)
}
