package com.opendatamask.domain.port.input

import com.opendatamask.domain.model.Webhook
import com.opendatamask.service.WebhookRequest

interface WebhookUseCase {
    fun listWebhooks(workspaceId: Long): List<Webhook>
    fun createWebhook(workspaceId: Long, request: WebhookRequest): Webhook
    fun updateWebhook(workspaceId: Long, webhookId: Long, request: WebhookRequest): Webhook
    fun deleteWebhook(workspaceId: Long, webhookId: Long)
    fun testWebhook(workspaceId: Long, webhookId: Long)
}
