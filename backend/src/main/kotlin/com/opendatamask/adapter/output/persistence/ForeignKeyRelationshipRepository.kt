package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.ForeignKeyRelationship
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ForeignKeyRelationshipRepository : JpaRepository<ForeignKeyRelationship, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<ForeignKeyRelationship>
    fun deleteByWorkspaceId(workspaceId: Long)
}
