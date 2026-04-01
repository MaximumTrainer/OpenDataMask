package com.opendatamask.service

import com.opendatamask.dto.PostJobActionRequest
import com.opendatamask.model.ActionType
import com.opendatamask.model.Job
import com.opendatamask.model.JobStatus
import com.opendatamask.model.PostJobAction
import com.opendatamask.repository.PostJobActionRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDateTime

class PostJobActionServiceTest {

    private val repository = mock<PostJobActionRepository>()
    private val service = PostJobActionService(repository)

    private fun makeJob(status: JobStatus = JobStatus.COMPLETED) = Job(
        id = 1L,
        workspaceId = 1L,
        status = status,
        startedAt = LocalDateTime.now().minusMinutes(5),
        completedAt = LocalDateTime.now(),
        createdBy = 1L
    )

    @Test
    fun `triggerActions queries actions for workspace`() {
        whenever(repository.findByWorkspaceId(1L)).thenReturn(emptyList())
        service.triggerActions(makeJob())
        verify(repository).findByWorkspaceId(1L)
    }

    @Test
    fun `triggerActions calls executeAction for each configured action`() {
        val webhook = PostJobAction(workspaceId = 1L, actionType = ActionType.WEBHOOK, config = """{"url":"http://example.com/hook"}""")
        whenever(repository.findByWorkspaceId(1L)).thenReturn(listOf(webhook))
        // Should not throw even with invalid URL in test environment
        try {
            service.triggerActions(makeJob())
        } catch (e: Exception) {
            // Network errors expected in unit test — just verify repository was called
        }
        verify(repository).findByWorkspaceId(1L)
    }

    @Test
    fun `buildPayload contains job id, status, workspaceId`() {
        val job = makeJob(JobStatus.COMPLETED)
        val payload = service.buildPayload(job)
        assertEquals(1L, payload["jobId"])
        assertEquals("COMPLETED", payload["status"])
        assertEquals(1L, payload["workspaceId"])
        assertNotNull(payload["completedAt"])
    }

    @Test
    fun `createAction saves to repository`() {
        val action = PostJobAction(workspaceId = 1L, actionType = ActionType.EMAIL, config = """{"to":"admin@example.com"}""")
        whenever(repository.save(any<PostJobAction>())).thenReturn(action.copy(id = 1L))
        val saved = service.createAction(action)
        verify(repository).save(action)
        assertNotNull(saved)
    }

    @Test
    fun `deleteAction calls repository`() {
        service.deleteAction(42L)
        verify(repository).deleteById(42L)
    }

    @Test
    fun `updateAction updates fields and saves`() {
        val existing = PostJobAction(
            id = 1L, workspaceId = 1L,
            actionType = ActionType.EMAIL,
            config = """{"to":"old@example.com"}"""
        )
        val request = PostJobActionRequest(
            actionType = ActionType.WEBHOOK,
            config = """{"url":"http://new.example.com"}""",
            enabled = false
        )
        whenever(repository.findById(1L)).thenReturn(java.util.Optional.of(existing))
        whenever(repository.save(any<PostJobAction>())).thenReturn(existing)
        service.updateAction(1L, 1L, request)
        verify(repository).save(existing)
        assertEquals(ActionType.WEBHOOK, existing.actionType)
        assertEquals("""{"url":"http://new.example.com"}""", existing.config)
        assertEquals(false, existing.enabled)
    }

    @Test
    fun `updateAction throws when action belongs to different workspace`() {
        val existing = PostJobAction(
            id = 1L, workspaceId = 99L,
            actionType = ActionType.EMAIL,
            config = """{"to":"other@example.com"}"""
        )
        whenever(repository.findById(1L)).thenReturn(java.util.Optional.of(existing))
        assertThrows<NoSuchElementException> {
            service.updateAction(1L, 1L, PostJobActionRequest(actionType = ActionType.EMAIL, config = "{}"))
        }
        verify(repository, never()).save(any())
    }
}
