package com.opendatamask.adapter.input.rest.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class JobRequest(
    @field:NotNull(message = "Workspace ID is required")
    val workspaceId: Long
)

