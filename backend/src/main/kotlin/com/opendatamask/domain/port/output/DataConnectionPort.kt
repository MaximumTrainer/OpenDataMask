package com.opendatamask.domain.port.output

import com.opendatamask.model.DataConnection
import java.util.Optional

interface DataConnectionPort {
    fun findById(id: Long): Optional<DataConnection>
    fun findByWorkspaceId(workspaceId: Long): List<DataConnection>
    fun findByWorkspaceIdAndIsSource(workspaceId: Long, isSource: Boolean): List<DataConnection>
    fun save(connection: DataConnection): DataConnection
    fun deleteById(id: Long)
    fun deleteByWorkspaceId(workspaceId: Long)
}
