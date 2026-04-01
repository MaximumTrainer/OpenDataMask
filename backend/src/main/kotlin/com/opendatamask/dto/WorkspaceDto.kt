package com.opendatamask.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.time.LocalDateTime

data class WorkspaceRequest(
    @field:NotBlank(message = "Workspace name is required")
    val name: String,
    val description: String? = null,
    val parentWorkspaceId: Long? = null,
    val inheritanceEnabled: Boolean = false
)

data class WorkspaceResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val ownerId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val parentWorkspaceId: Long? = null,
    val inheritanceEnabled: Boolean = false
)

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

data class WorkspaceUserRequest(
    val userId: Long,
    val role: String = "USER"
)

data class WorkspaceUserResponse(
    val id: Long,
    val workspaceId: Long,
    val userId: Long,
    val username: String,
    val email: String,
    val role: String
)
