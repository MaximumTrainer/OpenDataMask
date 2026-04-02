package com.opendatamask.domain.port.output

import com.opendatamask.model.StoredFile
import java.util.Optional

interface StoredFilePort {
    fun findById(id: Long): Optional<StoredFile>
    fun findByWorkspaceId(workspaceId: Long): List<StoredFile>
    fun findByWorkspaceIdAndIsSource(workspaceId: Long, isSource: Boolean): List<StoredFile>
    fun save(file: StoredFile): StoredFile
    fun deleteById(id: Long)
    fun deleteByWorkspaceId(workspaceId: Long)
}
