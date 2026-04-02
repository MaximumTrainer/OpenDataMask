package com.opendatamask.domain.port.input.dto

import com.opendatamask.domain.model.WebhookTriggerType
import com.opendatamask.domain.model.WebhookType

data class WebhookRequest(
    val name: String,
    val enabled: Boolean = true,
    val triggerType: WebhookTriggerType,
    val triggerEvents: Set<String> = emptySet(),
    val webhookType: WebhookType = WebhookType.CUSTOM,
    val url: String? = null,
    val bypassSsl: Boolean = false,
    val headersJson: String? = null,
    val bodyTemplate: String? = null,
    val githubOwner: String? = null,
    val githubRepo: String? = null,
    val githubPat: String? = null,
    val githubWorkflow: String? = null,
    val githubInputsJson: String? = null
)
