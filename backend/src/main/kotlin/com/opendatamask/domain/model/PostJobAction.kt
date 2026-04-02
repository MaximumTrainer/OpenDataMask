package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class ActionType {
    WEBHOOK, EMAIL, SCRIPT
}

@Entity
@Table(name = "post_job_actions")
data class PostJobAction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var actionType: ActionType,

    @Column(nullable = false, columnDefinition = "TEXT")
    var config: String = "{}",

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() { createdAt = LocalDateTime.now() }
}
