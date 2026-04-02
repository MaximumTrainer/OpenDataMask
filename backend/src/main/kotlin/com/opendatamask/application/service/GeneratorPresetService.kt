package com.opendatamask.application.service

import com.opendatamask.domain.port.input.GeneratorPresetUseCase

import com.opendatamask.adapter.input.rest.dto.ColumnGeneratorResponse
import com.opendatamask.adapter.input.rest.dto.GeneratorPresetRequest
import com.opendatamask.adapter.input.rest.dto.GeneratorPresetResponse
import com.opendatamask.domain.model.GeneratorPreset
import com.opendatamask.adapter.output.persistence.ColumnGeneratorRepository
import com.opendatamask.adapter.output.persistence.GeneratorPresetRepository
import com.opendatamask.adapter.output.persistence.TableConfigurationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GeneratorPresetService(
    private val generatorPresetRepository: GeneratorPresetRepository,
    private val columnGeneratorRepository: ColumnGeneratorRepository,
    private val tableConfigurationRepository: TableConfigurationRepository
) : GeneratorPresetUseCase {

    @Transactional(readOnly = true)
    override fun listSystemPresets(): List<GeneratorPresetResponse> =
        generatorPresetRepository.findByIsSystemTrue().map { it.toResponse() }

    @Transactional(readOnly = true)
    override fun listWorkspacePresets(workspaceId: Long): List<GeneratorPresetResponse> =
        generatorPresetRepository.findByWorkspaceId(workspaceId).map { it.toResponse() }

    @Transactional
    override fun createPreset(workspaceId: Long, request: GeneratorPresetRequest): GeneratorPresetResponse {
        val preset = GeneratorPreset(
            name = request.name,
            generatorType = request.generatorType,
            generatorParams = request.generatorParams,
            workspaceId = workspaceId,
            isSystem = false
        )
        return generatorPresetRepository.save(preset).toResponse()
    }

    @Transactional
    override fun updatePreset(workspaceId: Long, presetId: Long, request: GeneratorPresetRequest): GeneratorPresetResponse {
        val existing = generatorPresetRepository.findById(presetId)
            .orElseThrow { NoSuchElementException("Preset not found: $presetId") }
        if (existing.workspaceId != workspaceId) {
            throw NoSuchElementException("Preset $presetId does not belong to workspace $workspaceId")
        }
        val updated = GeneratorPreset(
            id = existing.id,
            name = request.name,
            generatorType = request.generatorType,
            generatorParams = request.generatorParams,
            workspaceId = workspaceId,
            isSystem = false,
            createdAt = existing.createdAt
        )
        return generatorPresetRepository.save(updated).toResponse()
    }

    @Transactional
    override fun deletePreset(workspaceId: Long, presetId: Long) {
        val preset = generatorPresetRepository.findById(presetId)
            .orElseThrow { NoSuchElementException("Preset not found: $presetId") }
        if (preset.workspaceId != workspaceId) {
            throw NoSuchElementException("Preset $presetId does not belong to workspace $workspaceId")
        }
        generatorPresetRepository.delete(preset)
    }

    @Transactional
    override fun applyPreset(columnGeneratorId: Long, presetId: Long): ColumnGeneratorResponse {
        val generator = columnGeneratorRepository.findById(columnGeneratorId)
            .orElseThrow { NoSuchElementException("Column generator not found: $columnGeneratorId") }
        val preset = generatorPresetRepository.findById(presetId)
            .orElseThrow { NoSuchElementException("Preset not found: $presetId") }
        generator.presetId = presetId
        generator.generatorType = preset.generatorType
        generator.generatorParams = preset.generatorParams
        return columnGeneratorRepository.save(generator).toColumnResponse()
    }

    @Transactional
    override fun applyPresetToColumn(workspaceId: Long, tableName: String, columnName: String, presetId: Long): ColumnGeneratorResponse {
        val tableConfig = tableConfigurationRepository.findByWorkspaceIdAndTableName(workspaceId, tableName)
            .orElseThrow { NoSuchElementException("Table config not found for workspace $workspaceId, table $tableName") }
        val generator = columnGeneratorRepository.findByTableConfigurationId(tableConfig.id)
            .find { it.columnName == columnName }
            ?: throw NoSuchElementException("Column generator not found for column $columnName in table $tableName")
        return applyPreset(generator.id, presetId)
    }

    private fun GeneratorPreset.toResponse() = GeneratorPresetResponse(
        id = id ?: 0L,
        name = name,
        generatorType = generatorType,
        generatorParams = generatorParams,
        workspaceId = workspaceId,
        isSystem = isSystem,
        createdAt = createdAt
    )

    private fun com.opendatamask.domain.model.ColumnGenerator.toColumnResponse() = ColumnGeneratorResponse(
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