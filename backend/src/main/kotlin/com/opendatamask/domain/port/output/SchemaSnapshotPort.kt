package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.SchemaSnapshot

interface SchemaSnapshotPort {
    fun findTopByWorkspaceIdOrderBySnapshotAtDesc(workspaceId: Long): SchemaSnapshot?
    fun save(snapshot: SchemaSnapshot): SchemaSnapshot
}
