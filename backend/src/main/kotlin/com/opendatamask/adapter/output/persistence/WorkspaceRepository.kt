package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.Workspace
import com.opendatamask.domain.port.output.WorkspacePort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface WorkspaceRepository : JpaRepository<Workspace, Long>, WorkspacePort {
    override fun findById(id: Long): Optional<Workspace>
    override fun findByOwnerId(ownerId: Long): List<Workspace>
    override fun findByParentWorkspaceId(parentWorkspaceId: Long): List<Workspace>
    override fun save(workspace: Workspace): Workspace
    override fun deleteById(id: Long)
}
