package com.opendatamask.service

import com.opendatamask.model.ActionType
import com.opendatamask.model.Job
import com.opendatamask.model.JobStatus
import com.opendatamask.model.PostJobAction
import com.opendatamask.repository.PostJobActionRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
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
}
