package com.opendatamask.domain.port.input

import com.opendatamask.adapter.input.rest.dto.SubsetTableConfigRequest
import com.opendatamask.domain.model.SubsetTableConfig

interface SubsetConfigUseCase {
    fun listConfigs(workspaceId: Long): List<SubsetTableConfig>
    fun createOrUpdateConfig(workspaceId: Long, request: SubsetTableConfigRequest): SubsetTableConfig
    fun updateConfig(workspaceId: Long, configId: Long, request: SubsetTableConfigRequest): SubsetTableConfig
    fun deleteConfig(workspaceId: Long, configId: Long)
}
