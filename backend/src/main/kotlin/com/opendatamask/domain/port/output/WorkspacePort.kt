package com.opendatamask.domain.port.output

import com.opendatamask.model.Workspace
import java.util.Optional

interface WorkspacePort {
    fun findById(id: Long): Optional<Workspace>
    fun findByOwnerId(ownerId: Long): List<Workspace>
    fun findByParentWorkspaceId(parentWorkspaceId: Long): List<Workspace>
    fun save(workspace: Workspace): Workspace
    fun deleteById(id: Long)
    fun findAll(): List<Workspace>
}
