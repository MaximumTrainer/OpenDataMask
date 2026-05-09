package com.opendatamask.domain.port.input.dto

import com.opendatamask.domain.model.JobStatus
import com.opendatamask.domain.model.LogLevel
import java.time.LocalDateTime

data class JobResponse(
    val id: Long,
    val workspaceId: Long,
    val status: JobStatus,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val errorMessage: String?,
    val createdBy: Long,
    val connectionPairId: Long? = null,
    val rowsProcessed: Long = 0,
    val tablesProcessed: Int = 0,
    val tablesTotal: Int = 0,
    val name: String? = null,
    val dryRun: Boolean = false,
    val dryRunReport: DryRunReport? = null,
    val sourceConnectionId: Long? = null,
    val targetConnectionId: Long? = null,
    val sourceConnectionName: String? = null,
    val targetConnectionName: String? = null
)

data class JobLogResponse(
    val id: Long,
    val jobId: Long,
    val message: String,
    val level: LogLevel,
    val timestamp: LocalDateTime
)

// Optional request body for job creation; all fields are nullable for backward compatibility.
// When connectionPairId is null/omitted, the job falls back to the workspace-wide source/destination lookup.
data class CreateJobRequest(
    val connectionPairId: Long? = null,
    val name: String? = null,
    // When true the job reads and masks data but does not write to the destination.
    // A DryRunReport is returned alongside the JobResponse.
    val dryRun: Boolean = false
)

data class DryRunTableReport(
    val tableName: String,
    val estimatedRowCount: Int,
    val sampleMaskedRows: List<Map<String, Any?>>
)

data class DryRunReport(
    val jobId: Long,
    val tables: List<DryRunTableReport>
)

data class JobTableStatsResponse(
    val id: Long,
    val jobId: Long,
    val tableName: String,
    val rowsRead: Long,
    val rowsWritten: Long,
    val rowsSkipped: Long,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val elapsedMs: Long,
    val rowsPerSecond: Double?,
    val errorMessage: String?
)
