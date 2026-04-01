package com.opendatamask.repository

import com.opendatamask.model.ColumnComment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ColumnCommentRepository : JpaRepository<ColumnComment, Long> {
    fun findByWorkspaceIdAndTableNameAndColumnNameOrderByCreatedAtAsc(
        workspaceId: Long, tableName: String, columnName: String
    ): List<ColumnComment>
    fun deleteByWorkspaceId(workspaceId: Long)
}
