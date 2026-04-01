package com.opendatamask.repository

import com.opendatamask.model.StoredFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StoredFileRepository : JpaRepository<StoredFile, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<StoredFile>
    fun findByWorkspaceIdAndIsSource(workspaceId: Long, isSource: Boolean): List<StoredFile>
    fun deleteByWorkspaceId(workspaceId: Long)
}
