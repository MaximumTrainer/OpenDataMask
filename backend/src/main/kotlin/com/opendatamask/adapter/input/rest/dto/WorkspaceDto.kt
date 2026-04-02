package com.opendatamask.adapter.input.rest.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class CreateChildWorkspaceRequest(
    @field:NotBlank(message = "Workspace name is required")
    val name: String,
    val description: String? = null,
    val inheritanceEnabled: Boolean = true
)

data class InheritedConfigResponse(
    val id: Long,
    val childWorkspaceId: Long,
    val parentWorkspaceId: Long,
    val configType: String,
    val tableName: String,
    val columnName: String?,
    val inheritedEntityId: Long,
    val inheritedAt: Instant
)

