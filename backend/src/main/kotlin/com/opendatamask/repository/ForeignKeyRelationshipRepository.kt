package com.opendatamask.repository

import com.opendatamask.model.ForeignKeyRelationship
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ForeignKeyRelationshipRepository : JpaRepository<ForeignKeyRelationship, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<ForeignKeyRelationship>
    fun deleteByWorkspaceId(workspaceId: Long)
}
