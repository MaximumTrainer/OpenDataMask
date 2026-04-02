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
    val createdBy: Long
)

data class JobLogResponse(
    val id: Long,
    val jobId: Long,
    val message: String,
    val level: LogLevel,
    val timestamp: LocalDateTime
)
