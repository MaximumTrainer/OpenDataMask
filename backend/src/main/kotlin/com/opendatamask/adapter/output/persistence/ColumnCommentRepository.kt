package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.ColumnComment
import com.opendatamask.domain.port.output.ColumnCommentPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ColumnCommentRepository : JpaRepository<ColumnComment, Long>, ColumnCommentPort {
    override fun findById(id: Long): Optional<ColumnComment>
    override fun findByWorkspaceIdAndTableNameAndColumnNameOrderByCreatedAtAsc(
        workspaceId: Long, tableName: String, columnName: String
    ): List<ColumnComment>
    override fun save(comment: ColumnComment): ColumnComment
    override fun deleteById(id: Long)
    override fun deleteByWorkspaceId(workspaceId: Long)
}
