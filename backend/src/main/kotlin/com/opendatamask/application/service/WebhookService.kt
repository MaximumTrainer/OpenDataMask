package com.opendatamask.application.service

import com.opendatamask.domain.port.input.WebhookUseCase

import com.opendatamask.domain.port.output.EncryptionPort
import com.opendatamask.domain.port.input.dto.WebhookRequest
import com.opendatamask.domain.model.*
import com.opendatamask.domain.port.output.WebhookPort
import com.opendatamask.domain.port.output.WorkspacePort
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class WebhookService(
    private val webhookRepository: WebhookPort,
    private val workspaceRepository: WorkspacePort,
    private val encryptionPort: EncryptionPort,
    private val restTemplate: RestTemplate
) : WebhookUseCase {
    private val logger = LoggerFactory.getLogger(WebhookService::class.java)

    // ÔöÇÔöÇ CRUD ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ

    override fun listWebhooks(workspaceId: Long): List<Webhook> =
        webhookRepository.findByWorkspaceId(workspaceId)

    override fun createWebhook(workspaceId: Long, request: WebhookRequest): Webhook {
        val webhook = Webhook(
            workspaceId = workspaceId,
            name = request.name,
            enabled = request.enabled,
            triggerType = request.triggerType,
            triggerEvents = request.triggerEvents.toMutableSet(),
            webhookType = request.webhookType,
            url = request.url,
            bypassSsl = request.bypassSsl,
            headersJson = request.headersJson,
            bodyTemplate = request.bodyTemplate,
            githubOwner = request.githubOwner,
            githubRepo = request.githubRepo,
            githubPatEncrypted = request.githubPat?.let { encryptionPort.encrypt(it) },
            githubWorkflow = request.githubWorkflow,
            githubInputsJson = request.githubInputsJson
        )
        return webhookRepository.save(webhook)
    }

    override fun updateWebhook(workspaceId: Long, webhookId: Long, request: WebhookRequest): Webhook {
        val webhook = findWebhook(workspaceId, webhookId)
        webhook.name = request.name
        webhook.enabled = request.enabled
        webhook.url = request.url
        webhook.bypassSsl = request.bypassSsl
        webhook.headersJson = request.headersJson
        webhook.bodyTemplate = request.bodyTemplate
        webhook.githubOwner = request.githubOwner
        webhook.githubRepo = request.githubRepo
        if (request.githubPat != null) {
            webhook.githubPatEncrypted = encryptionPort.encrypt(request.githubPat)
        }
        webhook.githubWorkflow = request.githubWorkflow
        webhook.githubInputsJson = request.githubInputsJson
        return webhookRepository.save(webhook)
    }

    override fun deleteWebhook(workspaceId: Long, webhookId: Long) {
        findWebhook(workspaceId, webhookId)
        webhookRepository.deleteById(webhookId)
    }

    override fun testWebhook(workspaceId: Long, webhookId: Long) {
        val webhook = findWebhook(workspaceId, webhookId)
        val workspace = workspaceRepository.findById(workspaceId).orElse(null)
        val sampleVars = mapOf(
            "jobId" to "0",
            "jobStatus" to "COMPLETED",
            "jobType" to "FULL_GENERATION",
            "workspaceId" to workspaceId.toString(),
            "workspaceName" to (workspace?.name ?: "unknown"),
            "schemaChangesList" to "[]",
            "formattedMessage" to "Schema changes detected"
        )
        fire(webhook, sampleVars)
    }

    // ÔöÇÔöÇ Trigger: DATA_GENERATION ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ

    fun triggerForJob(job: MaskingJob, event: String) {
        val webhooks = webhookRepository.findByWorkspaceIdAndTriggerTypeAndEnabledTrue(
            job.workspaceId, WebhookTriggerType.DATA_GENERATION
        ).filter { event in it.triggerEvents }

        if (webhooks.isEmpty()) return

        val workspace = workspaceRepository.findById(job.workspaceId).orElse(null)
        val vars = mapOf(
            "jobId" to job.id.toString(),
            "jobStatus" to event,
            "jobType" to "FULL_GENERATION",
            "workspaceId" to job.workspaceId.toString(),
            "workspaceName" to (workspace?.name ?: "unknown"),
            "schemaChangesList" to "[]",
            "formattedMessage" to "Job ${job.id} $event"
        )
        webhooks.forEach { fire(it, vars) }
    }

    // ÔöÇÔöÇ Trigger: SCHEMA_CHANGE ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ

    fun triggerForSchemaChange(workspaceId: Long, changes: List<SchemaChange>) {
        if (changes.isEmpty()) return

        val webhooks = webhookRepository.findByWorkspaceIdAndTriggerTypeAndEnabledTrue(
            workspaceId, WebhookTriggerType.SCHEMA_CHANGE
        )
        if (webhooks.isEmpty()) return

        val workspace = workspaceRepository.findById(workspaceId).orElse(null)
        val changesList = changes.joinToString(", ") { "${it.changeType}(${it.tableName})" }
        val formatted = "Schema changes detected: $changesList"
        val vars = mapOf(
            "jobId" to "",
            "jobStatus" to "",
            "jobType" to "",
            "workspaceId" to workspaceId.toString(),
            "workspaceName" to (workspace?.name ?: "unknown"),
            "schemaChangesList" to changesList,
            "formattedMessage" to formatted
        )
        webhooks.forEach { fire(it, vars) }
    }

    // ÔöÇÔöÇ Internal fire ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ

    private fun fire(webhook: Webhook, vars: Map<String, String>) {
        try {
            when (webhook.webhookType) {
                WebhookType.CUSTOM -> fireCustom(webhook, vars)
                WebhookType.GITHUB_WORKFLOW -> fireGitHub(webhook, vars)
            }
        } catch (e: Exception) {
            logger.warn("Webhook ${webhook.id} (${webhook.name}) failed: ${e.message}")
        }
    }

    private fun fireCustom(webhook: Webhook, vars: Map<String, String>) {
        val url = webhook.url ?: return
        val body = substituteVars(webhook.bodyTemplate ?: "{}", vars)
        val headers = buildHeaders(webhook)
        val entity = HttpEntity(body, headers)
        restTemplate.exchange(url, HttpMethod.POST, entity, String::class.java)
    }

    private fun fireGitHub(webhook: Webhook, vars: Map<String, String>) {
        val owner = webhook.githubOwner ?: return
        val repo = webhook.githubRepo ?: return
        val workflow = webhook.githubWorkflow ?: return
        val patEncrypted = webhook.githubPatEncrypted ?: return
        val pat = encryptionPort.decrypt(patEncrypted)

        val url = "https://api.github.com/repos/$owner/$repo/actions/workflows/$workflow/dispatches"
        val inputsRaw = webhook.githubInputsJson ?: "{}"
        val inputs = substituteVars(inputsRaw, vars)
        val body = """{"ref":"main","inputs":$inputs}"""

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", "token $pat")
            set("Accept", "application/vnd.github+json")
        }
        val entity = HttpEntity(body, headers)
        restTemplate.exchange(url, HttpMethod.POST, entity, String::class.java)
    }

    private fun substituteVars(template: String, vars: Map<String, String>): String {
        var result = template
        vars.forEach { (key, value) -> result = result.replace("{$key}", value) }
        return result
    }

    private fun buildHeaders(webhook: Webhook): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        webhook.headersJson?.let { json ->
            try {
                val map = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                    .readValue(json, Map::class.java)
                @Suppress("UNCHECKED_CAST")
                (map as Map<String, String>).forEach { (k, v) -> headers.set(k, v) }
            } catch (e: Exception) {
                logger.warn("Cannot parse headersJson for webhook ${webhook.id}: ${e.message}")
            }
        }
        return headers
    }

    private fun findWebhook(workspaceId: Long, webhookId: Long): Webhook {
        val webhook = webhookRepository.findById(webhookId)
            .orElseThrow { NoSuchElementException("Webhook not found: $webhookId") }
        if (webhook.workspaceId != workspaceId) {
            throw NoSuchElementException("Webhook $webhookId does not belong to workspace $workspaceId")
        }
        return webhook
    }
}

// Type alias so existing Job class is unambiguous
typealias MaskingJob = com.opendatamask.domain.model.Job
