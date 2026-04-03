package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.Webhook
import com.opendatamask.domain.model.WebhookTriggerType
import com.opendatamask.domain.port.output.WebhookPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface WebhookRepository : JpaRepository<Webhook, Long>, WebhookPort {
    override fun findById(id: Long): Optional<Webhook>
    override fun findByWorkspaceId(workspaceId: Long): List<Webhook>
    override fun findByWorkspaceIdAndTriggerTypeAndEnabledTrue(workspaceId: Long, triggerType: WebhookTriggerType): List<Webhook>
    override fun save(webhook: Webhook): Webhook
    override fun deleteById(id: Long)
}
