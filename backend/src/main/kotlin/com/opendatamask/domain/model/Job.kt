package com.opendatamask.domain.model

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
    var createdBy: Long,

    // Optional reference to a ConnectionPair; when set, the job uses that pair's
    // source and destination connections instead of the workspace defaults.
    @Column
    var connectionPairId: Long? = null,

    @Column
    var rowsProcessed: Long? = 0L,

    @Column
    var tablesProcessed: Int? = 0,

    @Column
    var tablesTotal: Int? = 0,

    @Column
    var name: String? = null
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
    }
}
