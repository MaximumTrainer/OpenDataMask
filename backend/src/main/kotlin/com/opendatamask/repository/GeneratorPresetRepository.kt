package com.opendatamask.repository

import com.opendatamask.domain.model.GeneratorPreset
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GeneratorPresetRepository : JpaRepository<GeneratorPreset, Long> {
    fun findByIsSystemTrue(): List<GeneratorPreset>
    fun findByWorkspaceId(workspaceId: Long): List<GeneratorPreset>
    fun existsByNameAndIsSystemTrue(name: String): Boolean
}
