package com.opendatamask.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.opendatamask.domain.model.ActionType
import com.opendatamask.domain.model.Job
import com.opendatamask.domain.model.PostJobAction
import com.opendatamask.repository.PostJobActionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
class PostJobActionService(
    private val repository: PostJobActionRepository
) {
    private val logger = LoggerFactory.getLogger(PostJobActionService::class.java)
    private val mapper = jacksonObjectMapper()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun triggerActions(job: Job) {
        val actions = repository.findByWorkspaceId(job.workspaceId).filter { it.enabled }
        val payload = buildPayload(job)
        for (action in actions) {
            try {
                executeAction(action, payload)
            } catch (e: Exception) {
                logger.error("Post-job action ${action.id} (${action.actionType}) failed: ${e.message}", e)
            }
        }
    }

    fun buildPayload(job: Job): Map<String, Any?> = mapOf(
        "jobId" to job.id,
        "workspaceId" to job.workspaceId,
        "status" to job.status.name,
        "startedAt" to job.startedAt?.toString(),
        "completedAt" to job.completedAt?.toString()
    )

    private fun executeAction(action: PostJobAction, payload: Map<String, Any?>) {
        val config: Map<String, String> = try {
            mapper.readValue(action.config)
        } catch (e: Exception) {
            emptyMap()
        }
        when (action.actionType) {
            ActionType.WEBHOOK -> executeWebhook(config, payload)
            ActionType.EMAIL -> executeEmail(config, payload)
            ActionType.SCRIPT -> executeScript(config, payload)
        }
    }

    private fun executeWebhook(config: Map<String, String>, payload: Map<String, Any?>) {
        val url = config["url"] ?: run {
            logger.warn("WEBHOOK action missing 'url' in config")
            return
        }
        val body = mapper.writeValueAsString(payload)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        logger.info("Webhook to $url responded with HTTP ${response.statusCode()}")
    }

    private fun executeEmail(config: Map<String, String>, payload: Map<String, Any?>) {
        val to = config["to"] ?: run {
            logger.warn("EMAIL action missing 'to' in config")
            return
        }
        // Email sending requires spring-boot-starter-mail and SMTP config.
        // Log intent; actual sending deferred to when SMTP config is present.
        logger.info("EMAIL action: would send job completion notification to $to for job ${payload["jobId"]}")
    }

    private fun executeScript(config: Map<String, String>, payload: Map<String, Any?>) {
        val path = config["path"] ?: run {
            logger.warn("SCRIPT action missing 'path' in config")
            return
        }
        logger.info("SCRIPT action: executing $path for job ${payload["jobId"]}")
        val process = ProcessBuilder(path)
            .redirectErrorStream(true)
            .start()
        val exited = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)
        if (!exited) {
            process.destroyForcibly()
            logger.warn("SCRIPT $path did not complete within 60 seconds; killed")
        } else {
            logger.info("SCRIPT $path exited with code ${process.exitValue()}")
        }
    }

    fun createAction(action: PostJobAction): PostJobAction = repository.save(action)
    fun listActions(workspaceId: Long): List<PostJobAction> = repository.findByWorkspaceId(workspaceId)
    fun updateAction(workspaceId: Long, id: Long, request: com.opendatamask.dto.PostJobActionRequest): PostJobAction {
        val existing = repository.findById(id)
            .orElseThrow { NoSuchElementException("PostJobAction not found: $id") }
        if (existing.workspaceId != workspaceId) {
            throw NoSuchElementException("PostJobAction $id does not belong to workspace $workspaceId")
        }
        existing.actionType = request.actionType
        existing.config = request.config
        existing.enabled = request.enabled
        return repository.save(existing)
    }
    fun deleteAction(id: Long) = repository.deleteById(id)
}
