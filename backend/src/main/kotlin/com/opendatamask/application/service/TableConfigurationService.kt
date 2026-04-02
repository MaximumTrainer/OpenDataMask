package com.opendatamask.application.service

import com.opendatamask.domain.port.input.TableConfigurationUseCase

import com.opendatamask.domain.port.input.dto.*
import com.opendatamask.domain.model.ColumnGenerator
import com.opendatamask.domain.model.TableConfiguration
import com.opendatamask.adapter.output.persistence.ColumnGeneratorRepository
import com.opendatamask.adapter.output.persistence.TableConfigurationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TableConfigurationService(
    private val tableConfigurationRepository: TableConfigurationRepository,
    private val columnGeneratorRepository: ColumnGeneratorRepository
) : TableConfigurationUseCase {

    @Transactional
    override fun createTableConfiguration(workspaceId: Long, request: TableConfigurationRequest): TableConfigurationResponse {
        val config = TableConfiguration(
            workspaceId = workspaceId,
            tableName = request.tableName,
            schemaName = request.schemaName,
            mode = request.mode,
            rowLimit = request.rowLimit,
            whereClause = request.whereClause
        )
        return tableConfigurationRepository.save(config).toResponse()
    }

    @Transactional(readOnly = true)
    override fun getTableConfiguration(workspaceId: Long, tableId: Long): TableConfigurationResponse {
        return findTableConfig(workspaceId, tableId).toResponse()
    }

    @Transactional(readOnly = true)
    override fun listTableConfigurations(workspaceId: Long): List<TableConfigurationResponse> {
        return tableConfigurationRepository.findByWorkspaceId(workspaceId).map { it.toResponse() }
    }

    @Transactional
    override fun updateTableConfiguration(
        workspaceId: Long,
        tableId: Long,
        request: TableConfigurationRequest
    ): TableConfigurationResponse {
        val config = findTableConfig(workspaceId, tableId)
        config.tableName = request.tableName
        config.schemaName = request.schemaName
        config.mode = request.mode
        config.rowLimit = request.rowLimit
        config.whereClause = request.whereClause
        return tableConfigurationRepository.save(config).toResponse()
    }

    @Transactional
    override fun deleteTableConfiguration(workspaceId: Long, tableId: Long) {
        val config = findTableConfig(workspaceId, tableId)
        columnGeneratorRepository.deleteByTableConfigurationId(config.id)
        tableConfigurationRepository.delete(config)
    }

    @Transactional
    override fun addColumnGenerator(tableId: Long, request: ColumnGeneratorRequest): ColumnGeneratorResponse {
        tableConfigurationRepository.findById(tableId)
            .orElseThrow { NoSuchElementException("Table configuration not found: $tableId") }

        val generator = ColumnGenerator(
            tableConfigurationId = tableId,
            columnName = request.columnName,
            generatorType = request.generatorType,
            generatorParams = request.generatorParams,
            presetId = request.presetId,
            consistencyMode = request.consistencyMode,
            linkKey = request.linkKey
        )
        return columnGeneratorRepository.save(generator).toColumnResponse()
    }

    @Transactional(readOnly = true)
    override fun listColumnGenerators(tableId: Long): List<ColumnGeneratorResponse> {
        tableConfigurationRepository.findById(tableId)
            .orElseThrow { NoSuchElementException("Table configuration not found: $tableId") }
        return columnGeneratorRepository.findByTableConfigurationId(tableId).map { it.toColumnResponse() }
    }

    @Transactional
    override fun updateColumnGenerator(
        tableId: Long,
        generatorId: Long,
        request: ColumnGeneratorRequest
    ): ColumnGeneratorResponse {
        val generator = columnGeneratorRepository.findById(generatorId)
            .orElseThrow { NoSuchElementException("Column generator not found: $generatorId") }
        if (generator.tableConfigurationId != tableId) {
            throw NoSuchElementException("Generator $generatorId does not belong to table config $tableId")
        }
        generator.columnName = request.columnName
        generator.generatorType = request.generatorType
        generator.generatorParams = request.generatorParams
        generator.presetId = request.presetId
        generator.consistencyMode = request.consistencyMode
        generator.linkKey = request.linkKey
        return columnGeneratorRepository.save(generator).toColumnResponse()
    }

    @Transactional
    override fun deleteColumnGenerator(tableId: Long, generatorId: Long) {
        val generator = columnGeneratorRepository.findById(generatorId)
            .orElseThrow { NoSuchElementException("Column generator not found: $generatorId") }
        if (generator.tableConfigurationId != tableId) {
            throw NoSuchElementException("Generator $generatorId does not belong to table config $tableId")
        }
        columnGeneratorRepository.delete(generator)
    }

    private fun findTableConfig(workspaceId: Long, tableId: Long): TableConfiguration {
        val config = tableConfigurationRepository.findById(tableId)
            .orElseThrow { NoSuchElementException("Table configuration not found: $tableId") }
        if (config.workspaceId != workspaceId) {
            throw NoSuchElementException("Table config $tableId does not belong to workspace $workspaceId")
        }
        return config
    }

    private fun TableConfiguration.toResponse() = TableConfigurationResponse(
        id = id,
        workspaceId = workspaceId,
        tableName = tableName,
        schemaName = schemaName,
        mode = mode,
        rowLimit = rowLimit,
        whereClause = whereClause,
        createdAt = createdAt
    )

    private fun ColumnGenerator.toColumnResponse() = ColumnGeneratorResponse(
        id = id,
        tableConfigurationId = tableConfigurationId,
        columnName = columnName,
        generatorType = generatorType,
        generatorParams = generatorParams,
        presetId = presetId,
        consistencyMode = consistencyMode,
        linkKey = linkKey,
        createdAt = createdAt
    )
}
