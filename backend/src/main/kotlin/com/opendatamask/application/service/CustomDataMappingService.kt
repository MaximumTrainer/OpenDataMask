package com.opendatamask.application.service

import com.opendatamask.domain.model.CustomDataMapping
import com.opendatamask.domain.model.MappingAction
import com.opendatamask.domain.port.input.CustomDataMappingUseCase
import com.opendatamask.domain.port.input.dto.BulkCustomDataMappingRequest
import com.opendatamask.domain.port.input.dto.CustomDataMappingRequest
import com.opendatamask.domain.port.input.dto.CustomDataMappingResponse
import com.opendatamask.domain.port.output.CustomDataMappingPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomDataMappingService(
    private val customDataMappingRepository: CustomDataMappingPort
) : CustomDataMappingUseCase {

    @Transactional
    override fun createMapping(workspaceId: Long, request: CustomDataMappingRequest): CustomDataMappingResponse {
        val mapping = CustomDataMapping(
            workspaceId = workspaceId,
            connectionId = request.connectionId,
            tableName = request.tableName,
            columnName = request.columnName,
            action = request.action,
            maskingStrategy = if (request.action == MappingAction.MASK) request.maskingStrategy else null,
            fakeGeneratorType = if (request.action == MappingAction.MASK) request.fakeGeneratorType else null
        )
        return customDataMappingRepository.save(mapping).toResponse()
    }

    @Transactional(readOnly = true)
    override fun getMapping(workspaceId: Long, mappingId: Long): CustomDataMappingResponse =
        findMapping(workspaceId, mappingId).toResponse()

    @Transactional(readOnly = true)
    override fun listMappings(workspaceId: Long): List<CustomDataMappingResponse> =
        customDataMappingRepository.findByWorkspaceId(workspaceId).map { it.toResponse() }

    @Transactional(readOnly = true)
    override fun listMappingsForTable(
        workspaceId: Long,
        connectionId: Long,
        tableName: String
    ): List<CustomDataMappingResponse> =
        customDataMappingRepository
            .findByWorkspaceIdAndConnectionIdAndTableName(workspaceId, connectionId, tableName)
            .map { it.toResponse() }

    @Transactional
    override fun updateMapping(
        workspaceId: Long,
        mappingId: Long,
        request: CustomDataMappingRequest
    ): CustomDataMappingResponse {
        val mapping = findMapping(workspaceId, mappingId)
        mapping.connectionId = request.connectionId
        mapping.tableName = request.tableName
        mapping.columnName = request.columnName
        mapping.action = request.action
        mapping.maskingStrategy = if (request.action == MappingAction.MASK) request.maskingStrategy else null
        mapping.fakeGeneratorType = if (request.action == MappingAction.MASK) request.fakeGeneratorType else null
        return customDataMappingRepository.save(mapping).toResponse()
    }

    @Transactional
    override fun deleteMapping(workspaceId: Long, mappingId: Long) {
        val mapping = findMapping(workspaceId, mappingId)
        customDataMappingRepository.deleteById(mapping.id)
    }

    @Transactional
    override fun saveBulkMappings(
        workspaceId: Long,
        request: BulkCustomDataMappingRequest
    ): List<CustomDataMappingResponse> {
        customDataMappingRepository.deleteByWorkspaceIdAndConnectionIdAndTableName(
            workspaceId, request.connectionId, request.tableName
        )
        val mappings = request.columnMappings.map { entry ->
            CustomDataMapping(
                workspaceId = workspaceId,
                connectionId = request.connectionId,
                tableName = request.tableName,
                columnName = entry.columnName,
                action = entry.action,
                maskingStrategy = if (entry.action == MappingAction.MASK) entry.maskingStrategy else null,
                fakeGeneratorType = if (entry.action == MappingAction.MASK) entry.fakeGeneratorType else null
            )
        }
        return customDataMappingRepository.bulkSave(mappings).map { it.toResponse() }
    }

    private fun findMapping(workspaceId: Long, mappingId: Long): CustomDataMapping {
        val mapping = customDataMappingRepository.findById(mappingId)
            .orElseThrow { NoSuchElementException("Custom data mapping not found: $mappingId") }
        if (mapping.workspaceId != workspaceId) {
            throw NoSuchElementException("Mapping $mappingId does not belong to workspace $workspaceId")
        }
        return mapping
    }

    private fun CustomDataMapping.toResponse() = CustomDataMappingResponse(
        id = id,
        workspaceId = workspaceId,
        connectionId = connectionId,
        tableName = tableName,
        columnName = columnName,
        action = action,
        maskingStrategy = maskingStrategy,
        fakeGeneratorType = fakeGeneratorType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
