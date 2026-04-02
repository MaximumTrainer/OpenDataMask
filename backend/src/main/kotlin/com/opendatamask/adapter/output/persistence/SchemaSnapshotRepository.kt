package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.SchemaSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SchemaSnapshotRepository : JpaRepository<SchemaSnapshot, Long> {
    fun findTopByWorkspaceIdOrderBySnapshotAtDesc(workspaceId: Long): SchemaSnapshot?
}
