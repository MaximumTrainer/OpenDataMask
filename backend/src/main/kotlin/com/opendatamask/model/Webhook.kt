package com.opendatamask.model

import jakarta.persistence.*
import java.time.Instant

enum class WebhookTriggerType { DATA_GENERATION, SCHEMA_CHANGE }
enum class WebhookType { CUSTOM, GITHUB_WORKFLOW }

@Entity
@Table(name = "webhooks")
class Webhook(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val workspaceId: Long,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val triggerType: WebhookTriggerType,

    @ElementCollection
    @CollectionTable(name = "webhook_trigger_events", joinColumns = [JoinColumn(name = "webhook_id")])
    @Column(name = "event")
    val triggerEvents: MutableSet<String> = mutableSetOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val webhookType: WebhookType = WebhookType.CUSTOM,

    @Column
    var url: String? = null,

    @Column(nullable = false)
    var bypassSsl: Boolean = false,

    @Column(columnDefinition = "TEXT")
    var headersJson: String? = null,

    @Column(columnDefinition = "TEXT")
    var bodyTemplate: String? = null,

    @Column
    var githubOwner: String? = null,

    @Column
    var githubRepo: String? = null,

    @Column(columnDefinition = "TEXT")
    var githubPatEncrypted: String? = null,

    @Column
    var githubWorkflow: String? = null,

    @Column(columnDefinition = "TEXT")
    var githubInputsJson: String? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
