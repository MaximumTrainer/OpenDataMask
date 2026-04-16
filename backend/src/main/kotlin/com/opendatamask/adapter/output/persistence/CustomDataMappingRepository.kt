package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.CustomDataMapping
import com.opendatamask.domain.port.output.CustomDataMappingPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CustomDataMappingRepository : JpaRepository<CustomDataMapping, Long>, CustomDataMappingPort {
    override fun findById(id: Long): Optional<CustomDataMapping>
    override fun findByWorkspaceId(workspaceId: Long): List<CustomDataMapping>
    override fun findByWorkspaceIdAndConnectionIdAndTableName(
        workspaceId: Long,
        connectionId: Long,
        tableName: String
    ): List<CustomDataMapping>
    override fun save(mapping: CustomDataMapping): CustomDataMapping
    override fun bulkSave(mappings: List<CustomDataMapping>): List<CustomDataMapping> =
        saveAll(mappings)
    override fun deleteById(id: Long)
    override fun deleteByWorkspaceIdAndConnectionIdAndTableName(
        workspaceId: Long,
        connectionId: Long,
        tableName: String
    )
}
