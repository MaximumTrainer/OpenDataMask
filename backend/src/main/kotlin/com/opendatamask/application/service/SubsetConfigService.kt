package com.opendatamask.application.service

import com.opendatamask.domain.port.input.SubsetConfigUseCase

import com.opendatamask.domain.port.input.dto.SubsetTableConfigRequest
import com.opendatamask.domain.model.SubsetTableConfig
import com.opendatamask.domain.port.output.SubsetTableConfigPort
import org.springframework.stereotype.Service

@Service
class SubsetConfigService(
    private val subsetTableConfigRepository: SubsetTableConfigPort
) : SubsetConfigUseCase {
    override fun listConfigs(workspaceId: Long): List<SubsetTableConfig> =
        subsetTableConfigRepository.findByWorkspaceId(workspaceId)

    override fun createOrUpdateConfig(workspaceId: Long, request: SubsetTableConfigRequest): SubsetTableConfig {
        val existing = subsetTableConfigRepository.findByWorkspaceIdAndTableName(workspaceId, request.tableName)
        val config = existing ?: SubsetTableConfig(workspaceId = workspaceId, tableName = request.tableName)
        config.limitType = request.limitType
        config.limitValue = request.limitValue
        config.isTargetTable = request.isTargetTable
        config.isLookupTable = request.isLookupTable
        return subsetTableConfigRepository.save(config)
    }

    override fun updateConfig(workspaceId: Long, configId: Long, request: SubsetTableConfigRequest): SubsetTableConfig {
        val config = subsetTableConfigRepository.findById(configId)
            .orElseThrow { NoSuchElementException("SubsetTableConfig not found: $configId") }
        if (config.workspaceId != workspaceId) {
            throw NoSuchElementException("SubsetTableConfig $configId does not belong to workspace $workspaceId")
        }
        config.limitType = request.limitType
        config.limitValue = request.limitValue
        config.isTargetTable = request.isTargetTable
        config.isLookupTable = request.isLookupTable
        return subsetTableConfigRepository.save(config)
    }

    override fun deleteConfig(workspaceId: Long, configId: Long) {
        val config = subsetTableConfigRepository.findById(configId)
            .orElseThrow { NoSuchElementException("SubsetTableConfig not found: $configId") }
        if (config.workspaceId != workspaceId) {
            throw NoSuchElementException("SubsetTableConfig $configId does not belong to workspace $workspaceId")
        }
        subsetTableConfigRepository.deleteById(configId)
    }
}
