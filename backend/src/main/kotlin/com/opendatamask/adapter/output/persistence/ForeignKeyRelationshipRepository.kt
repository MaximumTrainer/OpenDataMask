package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.ForeignKeyRelationship
import com.opendatamask.domain.port.output.ForeignKeyRelationshipPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ForeignKeyRelationshipRepository : JpaRepository<ForeignKeyRelationship, Long>, ForeignKeyRelationshipPort {
    override fun findById(id: Long): Optional<ForeignKeyRelationship>
    override fun findByWorkspaceId(workspaceId: Long): List<ForeignKeyRelationship>
    override fun save(relationship: ForeignKeyRelationship): ForeignKeyRelationship
    override fun deleteById(id: Long)
    override fun deleteByWorkspaceId(workspaceId: Long)
}
