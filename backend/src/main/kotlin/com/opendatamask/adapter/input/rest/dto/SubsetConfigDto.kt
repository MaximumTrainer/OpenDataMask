package com.opendatamask.adapter.input.rest.dto

import com.opendatamask.domain.model.SubsetLimitType
import com.opendatamask.domain.model.SubsetTableConfig

data class SubsetTableConfigResponse(
    val id: Long?,
    val workspaceId: Long,
    val tableName: String,
    val limitType: SubsetLimitType,
    val limitValue: Int,
    val isTargetTable: Boolean,
    val isLookupTable: Boolean
)

fun SubsetTableConfig.toResponse() = SubsetTableConfigResponse(
    id = id,
    workspaceId = workspaceId,
    tableName = tableName,
    limitType = limitType,
    limitValue = limitValue,
    isTargetTable = isTargetTable,
    isLookupTable = isLookupTable
)

