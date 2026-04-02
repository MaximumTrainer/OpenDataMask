package com.opendatamask.dto

import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.model.TableMode
import com.opendatamask.domain.model.ActionType

data class WorkspaceConfigDto(
    val version: String = "1.0",
    val tables: List<TableConfigExportDto> = emptyList(),
    val actions: List<ActionExportDto> = emptyList()
)

data class TableConfigExportDto(
    val tableName: String,
    val schemaName: String?,
    val mode: TableMode,
    val rowLimit: Long?,
    val whereClause: String?,
    val columnGenerators: List<ColumnGeneratorExportDto> = emptyList()
)

data class ColumnGeneratorExportDto(
    val columnName: String,
    val generatorType: GeneratorType,
    val generatorParams: String?
)

data class ActionExportDto(
    val actionType: ActionType,
    val config: String,
    val enabled: Boolean
)
