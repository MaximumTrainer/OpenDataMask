package com.opendatamask.domain.port.input.dto

import com.opendatamask.domain.model.SubsetLimitType
import jakarta.validation.constraints.NotBlank

data class SubsetTableConfigRequest(
    @field:NotBlank val tableName: String,
    val limitType: SubsetLimitType = SubsetLimitType.PERCENTAGE,
    val limitValue: Int = 10,
    val isTargetTable: Boolean = false,
    val isLookupTable: Boolean = false
)
