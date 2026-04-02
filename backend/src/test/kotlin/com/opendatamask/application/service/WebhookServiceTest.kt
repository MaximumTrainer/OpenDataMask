package com.opendatamask.application.service

import com.opendatamask.infrastructure.config.EncryptionService
import com.opendatamask.domain.model.*
import com.opendatamask.adapter.output.persistence.WebhookRepository
import com.opendatamask.adapter.output.persistence.WorkspaceRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class WebhookServiceTest {

    @Mock private lateinit var webhookRepository: WebhookRepository
    @Mock private lateinit var workspaceRepository: WorkspaceRepository
    @Mock private lateinit var encryptionService: EncryptionService
    @Mock private lateinit var restTemplate: RestTemplate

    @InjectMocks
    private lateinit var webhookService: WebhookService

    private fun makeWorkspace(id: Long = 1L) = Workspace(id = id, name = "Test WS", ownerId = 1L)

    private fun makeWebhook(
        id: Long = 1L,
        workspaceId: Long = 1L,
        triggerType: WebhookTriggerType = WebhookTriggerType.DATA_GENERATION,
        webhookType: WebhookType = WebhookType.CUSTOM,
        enabled: Boolean = true,
        triggerEvents: Set<String> = setOf("COMPLETED", "FAILED"),
        url: String? = "https://example.com/hook",
        bodyTemplate: String? = """{"job":"{jobId}","status":"{jobStatus}"}"""
    ) = Webhook(
        id = id,
        workspaceId = workspaceId,
        name = "Test hook",
        enabled = enabled,
        triggerType = triggerType,
        triggerEvents = triggerEvents.toMutableSet(),
        webhookType = webhookType,
        url = url,
        bodyTemplate = bodyTemplate
    )

    private fun makeJob(id: Long = 1L, workspaceId: Long = 1L, status: JobStatus = JobStatus.COMPLETED) =
        Job(id = id, workspaceId = workspaceId, status = status, createdBy = 1L)

    // ── triggerForJob ──────────────────────────────────────────────────────

    @Test
    fun `triggerForJob fires enabled webhooks with matching event`() {
        val job = makeJob()
        val webhook = makeWebhook(triggerEvents = setOf("COMPLETED"))
        whenever(webhookRepository.findByWorkspaceIdAndTriggerTypeAndEnabledTrue(1L, WebhookTriggerType.DATA_GENERATION))
            .thenReturn(listOf(webhook))
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))
        whenever(restTemplate.exchange(any<String>(), eq(HttpMethod.POST), any<HttpEntity<String>>(), eq(String::class.java)))
            .thenReturn(ResponseEntity.ok("ok"))

        webhookService.triggerForJob(job, "COMPLETED")

        verify(restTemplate).exchange(eq("https://example.com/hook"), eq(HttpMethod.POST), any<HttpEntity<String>>(), eq(String::class.java))
    }

    @Test
    fun `triggerForJob substitutes template variables`() {
        val job = makeJob(id = 42L, workspaceId = 1L)
        val webhook = makeWebhook(
            triggerEvents = setOf("COMPLETED"),
            bodyTemplate = """{"jobId":"{jobId}","status":"{jobStatus}","ws":"{workspaceId}"}"""
        )
        whenever(webhookRepository.findByWorkspaceIdAndTriggerTypeAndEnabledTrue(1L, WebhookTriggerType.DATA_GENERATION))
            .thenReturn(listOf(webhook))
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))

        var capturedBody: String? = null
        whenever(restTemplate.exchange(any<String>(), eq(HttpMethod.POST), any<HttpEntity<String>>(), eq(String::class.java)))
            .thenAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                val entity = invocation.arguments[2] as HttpEntity<String>
                capturedBody = entity.body
                ResponseEntity.ok("ok")
            }

        webhookService.triggerForJob(job, "COMPLETED")

        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("\"42\""), "Expected jobId=42 in body: $capturedBody")
        assertTrue(capturedBody!!.contains("COMPLETED"), "Expected status in body: $capturedBody")
        assertTrue(capturedBody!!.contains("\"1\""), "Expected workspaceId in body: $capturedBody")
    }

    @Test
    fun `triggerForJob does not fire when event not in triggerEvents`() {
        val job = makeJob()
        val webhook = makeWebhook(triggerEvents = setOf("FAILED")) // only FAILED, not COMPLETED
        whenever(webhookRepository.findByWorkspaceIdAndTriggerTypeAndEnabledTrue(1L, WebhookTriggerType.DATA_GENERATION))
            .thenReturn(listOf(webhook))
        // workspaceRepository is not called — service returns early after filtering empty webhooks

        webhookService.triggerForJob(job, "COMPLETED")

        verifyNoInteractions(restTemplate)
    }

    @Test
    fun `triggerForJob does not fire disabled webhooks`() {
        val job = makeJob()
        // disabled webhooks are filtered by the repository query (enabled=true)
        whenever(webhookRepository.findByWorkspaceIdAndTriggerTypeAndEnabledTrue(1L, WebhookTriggerType.DATA_GENERATION))
            .thenReturn(emptyList())

        webhookService.triggerForJob(job, "COMPLETED")

        verifyNoInteractions(restTemplate)
    }

    @Test
    fun `triggerForJob does nothing when no webhooks exist`() {
        val job = makeJob()
        whenever(webhookRepository.findByWorkspaceIdAndTriggerTypeAndEnabledTrue(1L, WebhookTriggerType.DATA_GENERATION))
            .thenReturn(emptyList())

        webhookService.triggerForJob(job, "COMPLETED")

        verifyNoInteractions(restTemplate)
    }

    // ── triggerForSchemaChange ─────────────────────────────────────────────

    @Test
    fun `triggerForSchemaChange fires schema change webhooks`() {
        val changes = listOf(
            SchemaChange(id = 1L, workspaceId = 1L, changeType = SchemaChangeType.NEW_COLUMN, tableName = "users")
        )
        val webhook = makeWebhook(
            triggerType = WebhookTriggerType.SCHEMA_CHANGE,
            bodyTemplate = """{"changes":"{schemaChangesList}","msg":"{formattedMessage}"}"""
        )
        whenever(webhookRepository.findByWorkspaceIdAndTriggerTypeAndEnabledTrue(1L, WebhookTriggerType.SCHEMA_CHANGE))
            .thenReturn(listOf(webhook))
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))
        whenever(restTemplate.exchange(any<String>(), eq(HttpMethod.POST), any<HttpEntity<String>>(), eq(String::class.java)))
            .thenReturn(ResponseEntity.ok("ok"))

        webhookService.triggerForSchemaChange(1L, changes)

        verify(restTemplate).exchange(any<String>(), eq(HttpMethod.POST), any<HttpEntity<String>>(), eq(String::class.java))
    }

    @Test
    fun `triggerForSchemaChange substitutes schemaChangesList and formattedMessage`() {
        val changes = listOf(
            SchemaChange(id = 1L, workspaceId = 1L, changeType = SchemaChangeType.NEW_COLUMN, tableName = "orders")
        )
        val webhook = makeWebhook(
            triggerType = WebhookTriggerType.SCHEMA_CHANGE,
            bodyTemplate = """{"changes":"{schemaChangesList}"}"""
        )
        whenever(webhookRepository.findByWorkspaceIdAndTriggerTypeAndEnabledTrue(1L, WebhookTriggerType.SCHEMA_CHANGE))
            .thenReturn(listOf(webhook))
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))

        var capturedBody: String? = null
        whenever(restTemplate.exchange(any<String>(), eq(HttpMethod.POST), any<HttpEntity<String>>(), eq(String::class.java)))
            .thenAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                val entity = invocation.arguments[2] as HttpEntity<String>
                capturedBody = entity.body
                ResponseEntity.ok("ok")
            }

        webhookService.triggerForSchemaChange(1L, changes)

        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("NEW_COLUMN"), "Expected change type in body: $capturedBody")
        assertTrue(capturedBody!!.contains("orders"), "Expected table name in body: $capturedBody")
    }

    @Test
    fun `triggerForSchemaChange does nothing when changes list is empty`() {
        webhookService.triggerForSchemaChange(1L, emptyList())

        verifyNoInteractions(webhookRepository)
        verifyNoInteractions(restTemplate)
    }

    // ── GitHub workflow dispatch ───────────────────────────────────────────

    @Test
    fun `fireGitHub sends correct URL and Authorization header`() {
        val job = makeJob()
        val githubWebhook = Webhook(
            id = 1L,
            workspaceId = 1L,
            name = "GH Deploy",
            enabled = true,
            triggerType = WebhookTriggerType.DATA_GENERATION,
            triggerEvents = mutableSetOf("COMPLETED"),
            webhookType = WebhookType.GITHUB_WORKFLOW,
            githubOwner = "myorg",
            githubRepo = "myrepo",
            githubPatEncrypted = "encrypted_pat_value",
            githubWorkflow = "deploy.yml",
            githubInputsJson = """{"env":"staging"}"""
        )
        whenever(webhookRepository.findByWorkspaceIdAndTriggerTypeAndEnabledTrue(1L, WebhookTriggerType.DATA_GENERATION))
            .thenReturn(listOf(githubWebhook))
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))
        whenever(encryptionService.decrypt("encrypted_pat_value")).thenReturn("ghp_secret_token")

        var capturedUrl: String? = null
        var capturedHeaders: org.springframework.http.HttpHeaders? = null
        whenever(restTemplate.exchange(any<String>(), eq(HttpMethod.POST), any<HttpEntity<String>>(), eq(String::class.java)))
            .thenAnswer { invocation ->
                capturedUrl = invocation.arguments[0] as String
                @Suppress("UNCHECKED_CAST")
                val entity = invocation.arguments[2] as HttpEntity<String>
                capturedHeaders = entity.headers
                ResponseEntity.ok("ok")
            }

        webhookService.triggerForJob(job, "COMPLETED")

        assertEquals(
            "https://api.github.com/repos/myorg/myrepo/actions/workflows/deploy.yml/dispatches",
            capturedUrl
        )
        assertNotNull(capturedHeaders)
        assertEquals("token ghp_secret_token", capturedHeaders!!.getFirst("Authorization"))
    }

    // ── testWebhook ────────────────────────────────────────────────────────

    @Test
    fun `testWebhook fires with placeholder values`() {
        val webhook = makeWebhook()
        whenever(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook))
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))
        whenever(restTemplate.exchange(any<String>(), eq(HttpMethod.POST), any<HttpEntity<String>>(), eq(String::class.java)))
            .thenReturn(ResponseEntity.ok("ok"))

        webhookService.testWebhook(1L, 1L)

        verify(restTemplate).exchange(any<String>(), eq(HttpMethod.POST), any<HttpEntity<String>>(), eq(String::class.java))
    }

    @Test
    fun `testWebhook throws when webhook not found`() {
        whenever(webhookRepository.findById(99L)).thenReturn(Optional.empty())

        org.junit.jupiter.api.assertThrows<NoSuchElementException> {
            webhookService.testWebhook(1L, 99L)
        }
    }

    @Test
    fun `testWebhook throws when webhook belongs to different workspace`() {
        val webhook = makeWebhook(workspaceId = 2L)
        whenever(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook))

        org.junit.jupiter.api.assertThrows<NoSuchElementException> {
            webhookService.testWebhook(1L, 1L)
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @Test
    fun `createWebhook saves and returns webhook`() {
        val request = WebhookRequest(
            name = "My Hook",
            triggerType = WebhookTriggerType.DATA_GENERATION,
            triggerEvents = setOf("COMPLETED"),
            url = "https://example.com/hook"
        )
        whenever(webhookRepository.save(any<Webhook>())).thenAnswer { it.arguments[0] as Webhook }

        val result = webhookService.createWebhook(1L, request)

        assertEquals("My Hook", result.name)
        assertEquals(1L, result.workspaceId)
        verify(webhookRepository).save(any<Webhook>())
    }

    @Test
    fun `deleteWebhook removes webhook`() {
        val webhook = makeWebhook()
        whenever(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook))

        webhookService.deleteWebhook(1L, 1L)

        verify(webhookRepository).deleteById(1L)
    }

    @Test
    fun `deleteWebhook throws when webhook not found`() {
        whenever(webhookRepository.findById(99L)).thenReturn(Optional.empty())

        org.junit.jupiter.api.assertThrows<NoSuchElementException> {
            webhookService.deleteWebhook(1L, 99L)
        }
    }
}
