package com.opendatamask.adapter.input.rest.dto

import jakarta.validation.constraints.NotNull

data class ApplyPresetRequest(
    @field:NotNull(message = "Preset ID is required")
    val presetId: Long
)

