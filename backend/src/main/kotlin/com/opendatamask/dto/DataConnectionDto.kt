package com.opendatamask.dto

import com.opendatamask.model.ConnectionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class DataConnectionRequest(
    @field:NotBlank(message = "Connection name is required")
    val name: String,

    @field:NotNull(message = "Connection type is required")
    val type: ConnectionType,

    @field:NotBlank(message = "Connection string is required")
    val connectionString: String,

    val username: String? = null,
    val password: String? = null,
    val database: String? = null,
    val isSource: Boolean = false,
    val isDestination: Boolean = false
)

data class DataConnectionResponse(
    val id: Long,
    val workspaceId: Long,
    val name: String,
    val type: ConnectionType,
    val username: String?,
    val database: String?,
    val isSource: Boolean,
    val isDestination: Boolean,
    val createdAt: LocalDateTime
)

data class ConnectionTestResult(
    val success: Boolean,
    val message: String
)
