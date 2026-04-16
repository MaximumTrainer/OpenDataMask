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
    val connectionPairId: Long? = null
)

data class JobLogResponse(
    val id: Long,
    val jobId: Long,
    val message: String,
    val level: LogLevel,
    val timestamp: LocalDateTime
)

// Optional request body for job creation; all fields are nullable for backward compatibility.
data class JobRequest(
    val connectionPairId: Long? = null
)
