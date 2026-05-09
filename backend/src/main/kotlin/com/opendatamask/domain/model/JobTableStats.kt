package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "job_table_stats",
    uniqueConstraints = [UniqueConstraint(columnNames = ["job_id", "table_name"])]
)
class JobTableStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "job_id", nullable = false)
    val jobId: Long,

    @Column(name = "table_name", nullable = false)
    val tableName: String,

    @Column(nullable = false)
    var rowsRead: Long = 0,

    @Column(nullable = false)
    var rowsWritten: Long = 0,

    @Column(nullable = false)
    var rowsSkipped: Long = 0,

    @Column(nullable = false)
    var startedAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var completedAt: LocalDateTime? = null,

    @Column(nullable = false)
    var elapsedMs: Long = 0,

    @Column
    var rowsPerSecond: Double? = null,

    @Column
    var errorMessage: String? = null
)
