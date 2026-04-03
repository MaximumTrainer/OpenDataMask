package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.SchemaSnapshot
import com.opendatamask.domain.port.output.SchemaSnapshotPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SchemaSnapshotRepository : JpaRepository<SchemaSnapshot, Long>, SchemaSnapshotPort {
    override fun findTopByWorkspaceIdOrderBySnapshotAtDesc(workspaceId: Long): SchemaSnapshot?
    override fun save(snapshot: SchemaSnapshot): SchemaSnapshot
}
