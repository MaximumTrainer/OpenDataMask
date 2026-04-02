package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.ColumnComment
import java.util.Optional

interface ColumnCommentPort {
    fun findById(id: Long): Optional<ColumnComment>
    fun findByWorkspaceIdAndTableNameAndColumnNameOrderByCreatedAtAsc(
        workspaceId: Long, tableName: String, columnName: String
    ): List<ColumnComment>
    fun save(comment: ColumnComment): ColumnComment
    fun deleteById(id: Long)
    fun deleteByWorkspaceId(workspaceId: Long)
}
