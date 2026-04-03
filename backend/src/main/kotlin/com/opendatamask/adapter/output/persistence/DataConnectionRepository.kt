package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.DataConnection
import com.opendatamask.domain.port.output.DataConnectionPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface DataConnectionRepository : JpaRepository<DataConnection, Long>, DataConnectionPort {
    override fun findById(id: Long): Optional<DataConnection>
    override fun findByWorkspaceId(workspaceId: Long): List<DataConnection>
    override fun findByWorkspaceIdAndIsSource(workspaceId: Long, isSource: Boolean): List<DataConnection>
    override fun save(connection: DataConnection): DataConnection
    override fun deleteById(id: Long)
    override fun deleteByWorkspaceId(workspaceId: Long)
}
