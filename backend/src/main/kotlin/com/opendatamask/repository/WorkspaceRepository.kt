package com.opendatamask.repository

import com.opendatamask.model.Workspace
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceRepository : JpaRepository<Workspace, Long> {
    fun findByOwnerId(ownerId: Long): List<Workspace>
}
