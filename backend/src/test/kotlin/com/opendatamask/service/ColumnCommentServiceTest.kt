package com.opendatamask.service

import com.opendatamask.model.ColumnComment
import com.opendatamask.repository.ColumnCommentRepository
import com.opendatamask.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.Optional

class ColumnCommentServiceTest {

    private val commentRepo = mock<ColumnCommentRepository>()
    private val userRepo = mock<UserRepository>()
    private val service = ColumnCommentService(commentRepo, userRepo)

    @Test
    fun `getComments returns comments for column`() {
        val comment = ColumnComment(workspaceId = 1L, tableName = "users", columnName = "email", userId = 1L, comment = "PII field")
        whenever(commentRepo.findByWorkspaceIdAndTableNameAndColumnNameOrderByCreatedAtAsc(1L, "users", "email"))
            .thenReturn(listOf(comment))
        val result = service.getComments(1L, "users", "email")
        assertEquals(1, result.size)
        assertEquals("PII field", result[0].comment)
    }

    @Test
    fun `addComment saves and returns comment`() {
        val expectedComment = ColumnComment(workspaceId = 1L, tableName = "users", columnName = "email", userId = 1L, comment = "This column contains PII")
        whenever(commentRepo.save(any<ColumnComment>())).thenReturn(expectedComment)
        val result = service.addComment(1L, "users", "email", 1L, "This column contains PII")
        verify(commentRepo).save(any())
        assertEquals("This column contains PII", result.comment)
    }

    @Test
    fun `addComment throws on blank comment`() {
        assertThrows<IllegalArgumentException> {
            service.addComment(1L, "users", "email", 1L, "  ")
        }
    }

    @Test
    fun `deleteComment removes by id when user is owner`() {
        val comment = ColumnComment(id = 1L, workspaceId = 1L, tableName = "t", columnName = "c", userId = 42L, comment = "test")
        whenever(commentRepo.findById(1L)).thenReturn(Optional.of(comment))
        service.deleteComment(1L, 42L)
        verify(commentRepo).deleteById(1L)
    }

    @Test
    fun `deleteComment throws when user is not owner`() {
        val comment = ColumnComment(id = 1L, workspaceId = 1L, tableName = "t", columnName = "c", userId = 42L, comment = "test")
        whenever(commentRepo.findById(1L)).thenReturn(Optional.of(comment))
        assertThrows<IllegalArgumentException> {
            service.deleteComment(1L, requestingUserId = 99L)
        }
    }
}
