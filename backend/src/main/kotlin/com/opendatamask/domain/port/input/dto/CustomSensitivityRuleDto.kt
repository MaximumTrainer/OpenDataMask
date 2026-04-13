package com.opendatamask.domain.port.input.dto

import com.opendatamask.domain.model.GenericDataType
import com.opendatamask.domain.model.MatcherType
import java.time.Instant

data class CustomRuleMatcherDto(
    val matcherType: MatcherType,
    val value: String,
    val caseSensitive: Boolean = false
)

data class CustomSensitivityRuleRequest(
    val name: String,
    val description: String? = null,
    val dataTypeFilter: GenericDataType = GenericDataType.ANY,
    val matchers: List<CustomRuleMatcherDto> = emptyList(),
    val linkedPresetId: Long? = null,
    val isActive: Boolean = true
)

data class CustomSensitivityRuleResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val dataTypeFilter: GenericDataType,
    val matchers: List<CustomRuleMatcherDto>,
    val linkedPresetId: Long?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CustomRulePreviewRequest(
    val workspaceId: Long,
    val dataTypeFilter: GenericDataType = GenericDataType.ANY,
    val matchers: List<CustomRuleMatcherDto> = emptyList()
)

data class CustomRulePreviewResult(
    val tableName: String,
    val columnName: String,
    val columnType: String
)
