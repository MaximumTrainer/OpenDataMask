package com.opendatamask.domain.port.input.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class ConnectionPairRequest(
    @field:NotBlank(message = "Connection pair name is required")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Source connection ID is required")
    val sourceConnectionId: Long,

    @field:NotNull(message = "Destination connection ID is required")
    val destinationConnectionId: Long
)

data class ConnectionPairResponse(
    val id: Long,
    val workspaceId: Long,
    val name: String,
    val description: String?,
    val sourceConnectionId: Long,
    val destinationConnectionId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
