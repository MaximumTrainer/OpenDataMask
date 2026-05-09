package com.opendatamask.domain.port.input.dto

import com.opendatamask.domain.model.SubsetLimitType

data class TableEstimate(
    val tableName: String,
    val totalRows: Long,
    val estimatedRows: Long,
    val limitType: SubsetLimitType,
    val limitValue: Int
)

data class SubsetEstimateResponse(
    val workspaceId: Long,
    val totalEstimatedRows: Long,
    val success: Boolean,
    val errorMessage: String? = null,
    val tableEstimates: List<TableEstimate>
)
