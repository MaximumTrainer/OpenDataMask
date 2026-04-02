package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class ScheduledJobType {
    FULL_GENERATION, UPSERT
}

@Entity
@Table(name = "job_schedules")
class JobSchedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Column(nullable = false)
    var cronExpression: String,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var jobType: ScheduledJobType = ScheduledJobType.FULL_GENERATION,

    @Column
    var nextRunAt: LocalDateTime? = null,

    @Column
    var lastRunAt: LocalDateTime? = null,

    @Column
    var lastJobId: Long? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
