package com.opendatamask.domain.port.input

import com.opendatamask.domain.port.input.dto.BulkCustomDataMappingRequest
import com.opendatamask.domain.port.input.dto.CustomDataMappingRequest
import com.opendatamask.domain.port.input.dto.CustomDataMappingResponse

interface CustomDataMappingUseCase {
    fun createMapping(workspaceId: Long, request: CustomDataMappingRequest): CustomDataMappingResponse
    fun getMapping(workspaceId: Long, mappingId: Long): CustomDataMappingResponse
    fun listMappings(workspaceId: Long): List<CustomDataMappingResponse>
    fun listMappingsForTable(workspaceId: Long, connectionId: Long, tableName: String): List<CustomDataMappingResponse>
    fun updateMapping(workspaceId: Long, mappingId: Long, request: CustomDataMappingRequest): CustomDataMappingResponse
    fun deleteMapping(workspaceId: Long, mappingId: Long)
    fun saveBulkMappings(workspaceId: Long, request: BulkCustomDataMappingRequest): List<CustomDataMappingResponse>
}
