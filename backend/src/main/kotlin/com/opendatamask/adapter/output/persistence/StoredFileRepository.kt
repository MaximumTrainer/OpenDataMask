package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.StoredFile
import com.opendatamask.domain.port.output.StoredFilePort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface StoredFileRepository : JpaRepository<StoredFile, Long>, StoredFilePort {
    override fun findById(id: Long): Optional<StoredFile>
    override fun findByWorkspaceId(workspaceId: Long): List<StoredFile>
    override fun findByWorkspaceIdAndIsSource(workspaceId: Long, isSource: Boolean): List<StoredFile>
    override fun save(file: StoredFile): StoredFile
    override fun deleteById(id: Long)
    override fun deleteByWorkspaceId(workspaceId: Long)
}
