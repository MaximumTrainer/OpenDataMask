package com.opendatamask.adapter.input.rest.dto

import com.opendatamask.domain.model.GeneratorType

data class ColumnPreviewResult(
    val tableName: String,
    val columnName: String,
    val generatorType: GeneratorType?,
    val samples: List<PreviewSample>
)

data class PreviewSample(
    val originalValue: String?,
    val maskedValue: String?
)
