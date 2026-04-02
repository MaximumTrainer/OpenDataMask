package com.opendatamask.dto

import java.time.Instant

data class SensitivityScanLogResponse(
    val id: Long?,
    val workspaceId: Long,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val status: String,
    val scannedColumns: Int,
    val detectedColumns: Int,
    val entries: List<SensitivityScanLogEntryDto>
)

data class SensitivityScanLogEntryDto(
    val tableName: String,
    val columnName: String,
    val detectedType: String?,
    val confidenceLevel: String?,
    val recommendedGenerator: String?,
    val scannedAt: java.time.LocalDateTime
)
