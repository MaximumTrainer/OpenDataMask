package com.opendatamask.domain.port.input.dto

import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.model.MappingAction
import com.opendatamask.domain.model.MaskingStrategy
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CustomDataMappingRequest(
    @field:NotNull(message = "Connection ID is required")
    val connectionId: Long,

    @field:NotBlank(message = "Table name is required")
    val tableName: String,

    @field:NotBlank(message = "Column name is required")
    val columnName: String,

    @field:NotNull(message = "Action is required")
    val action: MappingAction,

    val maskingStrategy: MaskingStrategy? = null,

    val fakeGeneratorType: GeneratorType? = null,

    // JSON string carrying strategy-specific parameters.
    // HashRule: {"salt":"..."}
    // PartialMaskRule: {"keepFirst":"2","keepLast":"4","maskChar":"*"}
    // RegexRule: {"pattern":"\\d","replacement":"#"}
    val piiRuleParams: String? = null
)

data class BulkCustomDataMappingRequest(
    @field:NotNull(message = "Connection ID is required")
    val connectionId: Long,

    @field:NotBlank(message = "Table name is required")
    val tableName: String,

    @field:NotNull(message = "Column mappings are required")
    @field:Valid
    val columnMappings: List<ColumnMappingEntry>
) {
    data class ColumnMappingEntry(
        @field:NotBlank(message = "Column name is required")
        val columnName: String,

        @field:NotNull(message = "Action is required")
        val action: MappingAction,

        val maskingStrategy: MaskingStrategy? = null,

        val fakeGeneratorType: GeneratorType? = null,

        val piiRuleParams: String? = null
    )
}

data class CustomDataMappingResponse(
    val id: Long,
    val workspaceId: Long,
    val connectionId: Long,
    val tableName: String,
    val columnName: String,
    val action: MappingAction,
    val maskingStrategy: MaskingStrategy?,
    val fakeGeneratorType: GeneratorType?,
    val piiRuleParams: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ConnectionSchemaResponse(
    val connectionId: Long,
    val tables: List<TableSchemaInfo>
) {
    data class TableSchemaInfo(
        val tableName: String,
        val columns: List<ColumnSchemaInfo>
    )

    data class ColumnSchemaInfo(
        val name: String,
        val type: String,
        val nullable: Boolean
    )
}
