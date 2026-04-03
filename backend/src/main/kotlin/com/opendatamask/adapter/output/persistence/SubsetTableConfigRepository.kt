package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.SubsetTableConfig
import com.opendatamask.domain.port.output.SubsetTableConfigPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SubsetTableConfigRepository : JpaRepository<SubsetTableConfig, Long>, SubsetTableConfigPort {
    override fun findById(id: Long): Optional<SubsetTableConfig>
    override fun findByWorkspaceId(workspaceId: Long): List<SubsetTableConfig>
    override fun findByWorkspaceIdAndTableName(workspaceId: Long, tableName: String): SubsetTableConfig?
    override fun save(config: SubsetTableConfig): SubsetTableConfig
    override fun deleteById(id: Long)
    override fun deleteByWorkspaceId(workspaceId: Long)
}
