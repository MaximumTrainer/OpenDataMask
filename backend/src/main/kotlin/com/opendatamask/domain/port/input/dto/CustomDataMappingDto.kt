package com.opendatamask.domain.port.input.dto

import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.model.MappingAction
import com.opendatamask.domain.model.MaskingStrategy
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

    val fakeGeneratorType: GeneratorType? = null
)

data class BulkCustomDataMappingRequest(
    @field:NotNull(message = "Connection ID is required")
    val connectionId: Long,

    @field:NotBlank(message = "Table name is required")
    val tableName: String,

    @field:NotNull(message = "Column mappings are required")
    val columnMappings: List<ColumnMappingEntry>
) {
    data class ColumnMappingEntry(
        @field:NotBlank(message = "Column name is required")
        val columnName: String,

        @field:NotNull(message = "Action is required")
        val action: MappingAction,

        val maskingStrategy: MaskingStrategy? = null,

        val fakeGeneratorType: GeneratorType? = null
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
