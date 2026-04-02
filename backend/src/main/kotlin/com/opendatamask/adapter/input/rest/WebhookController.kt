package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.Webhook
import com.opendatamask.domain.port.input.dto.WebhookRequest
import com.opendatamask.application.service.WebhookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/webhooks")
class WebhookController(
    private val webhookService: WebhookService
) {
    @GetMapping
    fun list(@PathVariable workspaceId: Long): ResponseEntity<List<Webhook>> =
        ResponseEntity.ok(webhookService.listWebhooks(workspaceId))

    @PostMapping
    fun create(
        @PathVariable workspaceId: Long,
        @RequestBody request: WebhookRequest
    ): ResponseEntity<Webhook> =
        ResponseEntity.ok(webhookService.createWebhook(workspaceId, request))

    @PutMapping("/{webhookId}")
    fun update(
        @PathVariable workspaceId: Long,
        @PathVariable webhookId: Long,
        @RequestBody request: WebhookRequest
    ): ResponseEntity<Webhook> =
        ResponseEntity.ok(webhookService.updateWebhook(workspaceId, webhookId, request))

    @DeleteMapping("/{webhookId}")
    fun delete(
        @PathVariable workspaceId: Long,
        @PathVariable webhookId: Long
    ): ResponseEntity<Void> {
        webhookService.deleteWebhook(workspaceId, webhookId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{webhookId}/test")
    fun test(
        @PathVariable workspaceId: Long,
        @PathVariable webhookId: Long
    ): ResponseEntity<Void> {
        webhookService.testWebhook(workspaceId, webhookId)
        return ResponseEntity.ok().build()
    }
}

