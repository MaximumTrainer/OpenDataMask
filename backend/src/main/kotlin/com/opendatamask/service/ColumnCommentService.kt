package com.opendatamask.service

import com.opendatamask.domain.model.ColumnComment
import com.opendatamask.repository.ColumnCommentRepository
import com.opendatamask.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ColumnCommentService(
    private val commentRepository: ColumnCommentRepository,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(ColumnCommentService::class.java)

    fun getComments(workspaceId: Long, tableName: String, columnName: String): List<ColumnComment> =
        commentRepository.findByWorkspaceIdAndTableNameAndColumnNameOrderByCreatedAtAsc(workspaceId, tableName, columnName)

    fun addComment(workspaceId: Long, tableName: String, columnName: String, userId: Long, comment: String): ColumnComment {
        require(comment.isNotBlank()) { "Comment cannot be blank" }
        require(comment.length <= 2000) { "Comment too long (max 2000 chars)" }
        val saved = commentRepository.save(ColumnComment(
            workspaceId = workspaceId,
            tableName = tableName,
            columnName = columnName,
            userId = userId,
            comment = comment
        ))
        logger.info("New comment on $tableName.$columnName in workspace $workspaceId by user $userId")
        return saved
    }

    fun deleteComment(commentId: Long, requestingUserId: Long) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { NoSuchElementException("Comment not found: $commentId") }
        require(comment.userId == requestingUserId) { "Cannot delete another user's comment" }
        commentRepository.deleteById(commentId)
    }
}
