package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.SchemaChange
import com.opendatamask.domain.model.SchemaChangeStatus
import java.util.Optional

interface SchemaChangePort {
    fun findById(id: Long): Optional<SchemaChange>
    fun findByWorkspaceId(workspaceId: Long): List<SchemaChange>
    fun findByWorkspaceIdAndStatus(workspaceId: Long, status: SchemaChangeStatus): List<SchemaChange>
    fun save(change: SchemaChange): SchemaChange
    fun deleteByWorkspaceId(workspaceId: Long)
}
