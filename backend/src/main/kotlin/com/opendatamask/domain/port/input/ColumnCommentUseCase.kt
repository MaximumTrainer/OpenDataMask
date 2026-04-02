package com.opendatamask.domain.port.input

import com.opendatamask.model.ColumnComment

interface ColumnCommentUseCase {
    fun getComments(workspaceId: Long, tableName: String, columnName: String): List<ColumnComment>
    fun addComment(workspaceId: Long, tableName: String, columnName: String, userId: Long, comment: String): ColumnComment
    fun deleteComment(commentId: Long, requestingUserId: Long)
}
