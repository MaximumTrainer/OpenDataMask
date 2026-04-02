package com.opendatamask.adapter.input.rest.dto

import com.opendatamask.domain.model.GeneratorType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class GeneratorPresetRequest(
    @field:NotBlank(message = "Preset name is required")
    val name: String,

    @field:NotNull(message = "Generator type is required")
    val generatorType: GeneratorType,

    val generatorParams: String? = null
)

data class GeneratorPresetResponse(
    val id: Long,
    val name: String,
    val generatorType: GeneratorType,
    val generatorParams: String?,
    val workspaceId: Long?,
    val isSystem: Boolean,
    val createdAt: Instant
)

data class ApplyPresetRequest(
    @field:NotNull(message = "Preset ID is required")
    val presetId: Long
)
