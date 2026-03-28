package com.opendatamask.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class PostJobActionType {
    WEBHOOK, EMAIL, SCRIPT
}

@Entity
@Table(name = "post_job_actions")
class PostJobAction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: PostJobActionType,

    @Column(nullable = false, length = 8192)
    var configuration: String,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
    }
}
