package com.opendatamask.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class WorkspaceRequest(
    @field:NotBlank(message = "Workspace name is required")
    val name: String,
    val description: String? = null
)

data class WorkspaceResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val ownerId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
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
