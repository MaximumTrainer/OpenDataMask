package com.opendatamask.domain.port.input

import com.opendatamask.dto.ColumnGeneratorResponse
import com.opendatamask.dto.GeneratorPresetRequest
import com.opendatamask.dto.GeneratorPresetResponse

interface GeneratorPresetUseCase {
    fun listSystemPresets(): List<GeneratorPresetResponse>
    fun listWorkspacePresets(workspaceId: Long): List<GeneratorPresetResponse>
    fun createPreset(workspaceId: Long, request: GeneratorPresetRequest): GeneratorPresetResponse
    fun updatePreset(workspaceId: Long, presetId: Long, request: GeneratorPresetRequest): GeneratorPresetResponse
    fun deletePreset(workspaceId: Long, presetId: Long)
    fun applyPreset(columnGeneratorId: Long, presetId: Long): ColumnGeneratorResponse
    fun applyPresetToColumn(workspaceId: Long, tableName: String, columnName: String, presetId: Long): ColumnGeneratorResponse
}
