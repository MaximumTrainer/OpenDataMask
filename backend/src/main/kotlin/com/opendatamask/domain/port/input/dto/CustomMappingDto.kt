package com.opendatamask.domain.port.input.dto

import com.fasterxml.jackson.annotation.JsonProperty

enum class CustomMappingAction {
    MIGRATE_AS_IS, MASK
}

data class CustomMappingAttributeDto(
    val name: String,
    val action: CustomMappingAction,
    val strategy: String? = null
)

data class CustomMappingTableDto(
    @JsonProperty("table_name") val tableName: String,
    val attributes: List<CustomMappingAttributeDto> = emptyList()
)

data class CustomMappingDto(
    val project: String = "",
    val tables: List<CustomMappingTableDto> = emptyList()
)
