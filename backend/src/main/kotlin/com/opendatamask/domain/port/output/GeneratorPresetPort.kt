package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.GeneratorPreset
import java.util.Optional

interface GeneratorPresetPort {
    fun findById(id: Long): Optional<GeneratorPreset>
    fun findByIsSystemTrue(): List<GeneratorPreset>
    fun findByWorkspaceId(workspaceId: Long): List<GeneratorPreset>
    fun existsByNameAndIsSystemTrue(name: String): Boolean
    fun save(preset: GeneratorPreset): GeneratorPreset
    fun deleteById(id: Long)
}
