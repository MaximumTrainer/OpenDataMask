package com.opendatamask.repository

import com.opendatamask.model.SchemaSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SchemaSnapshotRepository : JpaRepository<SchemaSnapshot, Long> {
    fun findTopByWorkspaceIdOrderBySnapshotAtDesc(workspaceId: Long): SchemaSnapshot?
}
