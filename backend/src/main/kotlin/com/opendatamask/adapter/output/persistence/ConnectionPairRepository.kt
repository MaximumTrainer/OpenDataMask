package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.ConnectionPair
import com.opendatamask.domain.port.output.ConnectionPairPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ConnectionPairRepository : JpaRepository<ConnectionPair, Long>, ConnectionPairPort {
    override fun findById(id: Long): Optional<ConnectionPair>
    override fun findByWorkspaceId(workspaceId: Long): List<ConnectionPair>

    @Query("SELECT c FROM ConnectionPair c WHERE c.workspaceId = :workspaceId AND c.deletedAt IS NULL")
    override fun findActiveByWorkspaceId(workspaceId: Long): List<ConnectionPair>

    override fun save(connectionPair: ConnectionPair): ConnectionPair
    override fun deleteById(id: Long)
}
