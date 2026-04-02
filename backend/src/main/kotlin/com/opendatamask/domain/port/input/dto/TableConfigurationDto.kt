package com.opendatamask.domain.port.input.dto

import com.opendatamask.domain.model.ConsistencyMode
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.model.TableMode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class TableConfigurationRequest(
    @field:NotBlank(message = "Table name is required")
    val tableName: String,

    val schemaName: String? = null,

    @field:NotNull(message = "Table mode is required")
    val mode: TableMode,

    val rowLimit: Long? = null,
    val whereClause: String? = null
)

data class TableConfigurationResponse(
    val id: Long,
    val workspaceId: Long,
    val tableName: String,
    val schemaName: String?,
    val mode: TableMode,
    val rowLimit: Long?,
    val whereClause: String?,
    val createdAt: LocalDateTime
)

data class ColumnGeneratorRequest(
    @field:NotBlank(message = "Column name is required")
    val columnName: String,

    @field:NotNull(message = "Generator type is required")
    val generatorType: GeneratorType,

    val generatorParams: String? = null,
    val presetId: Long? = null,
    val consistencyMode: ConsistencyMode = ConsistencyMode.RANDOM,
    val linkKey: String? = null
)

data class ColumnGeneratorResponse(
    val id: Long,
    val tableConfigurationId: Long,
    val columnName: String,
    val generatorType: GeneratorType,
    val generatorParams: String?,
    val presetId: Long?,
    val consistencyMode: ConsistencyMode,
    val linkKey: String?,
    val createdAt: LocalDateTime
)
