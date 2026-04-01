package com.opendatamask.repository

import com.opendatamask.model.Webhook
import com.opendatamask.model.WebhookTriggerType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WebhookRepository : JpaRepository<Webhook, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<Webhook>
    fun findByWorkspaceIdAndTriggerTypeAndEnabledTrue(workspaceId: Long, triggerType: WebhookTriggerType): List<Webhook>
}
