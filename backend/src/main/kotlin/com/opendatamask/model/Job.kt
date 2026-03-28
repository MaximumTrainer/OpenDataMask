package com.opendatamask.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class JobStatus {
    PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
}

@Entity
@Table(name = "jobs")
class Job(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: JobStatus = JobStatus.PENDING,

    @Column
    var startedAt: LocalDateTime? = null,

    @Column
    var completedAt: LocalDateTime? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(length = 4096)
    var errorMessage: String? = null,

    @Column(nullable = false)
    var createdBy: Long
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
    }
}
