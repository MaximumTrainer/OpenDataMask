package com.opendatamask.service

import com.opendatamask.dto.*
import com.opendatamask.domain.model.*
import com.opendatamask.repository.*
import org.springframework.stereotype.Service

@Service
class WorkspaceExportService(
    private val tableConfigurationRepository: TableConfigurationRepository,
    private val columnGeneratorRepository: ColumnGeneratorRepository,
    private val postJobActionRepository: PostJobActionRepository,
    private val workspaceRepository: WorkspaceRepository
) {
    fun export(workspaceId: Long): WorkspaceConfigDto {
        val tables = tableConfigurationRepository.findByWorkspaceId(workspaceId)
        val tableDtos = tables.map { table ->
            val generators = columnGeneratorRepository.findByTableConfigurationId(table.id)
            TableConfigExportDto(
                tableName = table.tableName,
                schemaName = table.schemaName,
                mode = table.mode,
                rowLimit = table.rowLimit,
                whereClause = table.whereClause,
                columnGenerators = generators.map { gen ->
                    ColumnGeneratorExportDto(
                        columnName = gen.columnName,
                        generatorType = gen.generatorType,
                        generatorParams = gen.generatorParams
                    )
                }
            )
        }
        val actions = postJobActionRepository.findByWorkspaceId(workspaceId)
        val actionDtos = actions.map { action ->
            ActionExportDto(
                actionType = action.actionType,
                config = action.config,
                enabled = action.enabled
            )
        }
        return WorkspaceConfigDto(tables = tableDtos, actions = actionDtos)
    }

    fun import(workspaceId: Long, config: WorkspaceConfigDto) {
        workspaceRepository.findById(workspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $workspaceId") }

        val existingTables = tableConfigurationRepository.findByWorkspaceId(workspaceId)
            .associateBy { it.tableName }

        for (tableDto in config.tables) {
            val table = existingTables[tableDto.tableName] ?: TableConfiguration(
                workspaceId = workspaceId,
                tableName = tableDto.tableName
            )
            table.schemaName = tableDto.schemaName
            table.mode = tableDto.mode
            table.rowLimit = tableDto.rowLimit
            table.whereClause = tableDto.whereClause
            val savedTable = tableConfigurationRepository.save(table)

            val existingGenerators = columnGeneratorRepository.findByTableConfigurationId(savedTable.id)
                .associateBy { it.columnName }

            for (genDto in tableDto.columnGenerators) {
                val generator = existingGenerators[genDto.columnName] ?: ColumnGenerator(
                    tableConfigurationId = savedTable.id,
                    columnName = genDto.columnName,
                    generatorType = genDto.generatorType
                )
                generator.generatorType = genDto.generatorType
                generator.generatorParams = genDto.generatorParams
                columnGeneratorRepository.save(generator)
            }
        }

        for (actionDto in config.actions) {
            val action = PostJobAction(
                workspaceId = workspaceId,
                actionType = actionDto.actionType,
                config = actionDto.config,
                enabled = actionDto.enabled
            )
            postJobActionRepository.save(action)
        }
    }
}
