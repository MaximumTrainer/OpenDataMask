package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.GeneratorPreset
import com.opendatamask.domain.port.output.GeneratorPresetPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface GeneratorPresetRepository : JpaRepository<GeneratorPreset, Long>, GeneratorPresetPort {
    override fun findById(id: Long): Optional<GeneratorPreset>
    override fun findByIsSystemTrue(): List<GeneratorPreset>
    override fun findByWorkspaceId(workspaceId: Long): List<GeneratorPreset>
    override fun existsByNameAndIsSystemTrue(name: String): Boolean
    override fun save(preset: GeneratorPreset): GeneratorPreset
    override fun deleteById(id: Long)
}
