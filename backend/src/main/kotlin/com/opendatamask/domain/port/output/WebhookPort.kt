package com.opendatamask.domain.port.output

import com.opendatamask.model.Webhook
import com.opendatamask.model.WebhookTriggerType
import java.util.Optional

interface WebhookPort {
    fun findById(id: Long): Optional<Webhook>
    fun findByWorkspaceId(workspaceId: Long): List<Webhook>
    fun findByWorkspaceIdAndTriggerTypeAndEnabledTrue(workspaceId: Long, triggerType: WebhookTriggerType): List<Webhook>
    fun save(webhook: Webhook): Webhook
    fun deleteById(id: Long)
}
