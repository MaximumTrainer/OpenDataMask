package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.ForeignKeyRelationship
import java.util.Optional

interface ForeignKeyRelationshipPort {
    fun findById(id: Long): Optional<ForeignKeyRelationship>
    fun findByWorkspaceId(workspaceId: Long): List<ForeignKeyRelationship>
    fun save(relationship: ForeignKeyRelationship): ForeignKeyRelationship
    fun deleteById(id: Long)
    fun deleteByWorkspaceId(workspaceId: Long)
}
