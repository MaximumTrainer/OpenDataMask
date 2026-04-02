package com.opendatamask.domain.port.input

import com.opendatamask.adapter.input.rest.dto.ColumnGeneratorRequest
import com.opendatamask.adapter.input.rest.dto.ColumnGeneratorResponse
import com.opendatamask.adapter.input.rest.dto.TableConfigurationRequest
import com.opendatamask.adapter.input.rest.dto.TableConfigurationResponse

interface TableConfigurationUseCase {
    fun createTableConfiguration(workspaceId: Long, request: TableConfigurationRequest): TableConfigurationResponse
    fun getTableConfiguration(workspaceId: Long, tableId: Long): TableConfigurationResponse
    fun listTableConfigurations(workspaceId: Long): List<TableConfigurationResponse>
    fun updateTableConfiguration(workspaceId: Long, tableId: Long, request: TableConfigurationRequest): TableConfigurationResponse
    fun deleteTableConfiguration(workspaceId: Long, tableId: Long)
    fun addColumnGenerator(tableId: Long, request: ColumnGeneratorRequest): ColumnGeneratorResponse
    fun listColumnGenerators(tableId: Long): List<ColumnGeneratorResponse>
    fun updateColumnGenerator(tableId: Long, generatorId: Long, request: ColumnGeneratorRequest): ColumnGeneratorResponse
    fun deleteColumnGenerator(tableId: Long, generatorId: Long)
}
