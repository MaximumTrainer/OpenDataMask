package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.SchemaChange
import com.opendatamask.domain.model.SchemaChangeStatus
import com.opendatamask.domain.port.output.SchemaChangePort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SchemaChangeRepository : JpaRepository<SchemaChange, Long>, SchemaChangePort {
    override fun findById(id: Long): Optional<SchemaChange>
    override fun findByWorkspaceId(workspaceId: Long): List<SchemaChange>
    override fun findByWorkspaceIdAndStatus(workspaceId: Long, status: SchemaChangeStatus): List<SchemaChange>
    override fun save(change: SchemaChange): SchemaChange
    override fun deleteByWorkspaceId(workspaceId: Long)
}
