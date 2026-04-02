package com.opendatamask.dto

import com.opendatamask.domain.model.ForeignKeyRelationship
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class ForeignKeyRelationshipRequest(
    @field:NotBlank val fromTable: String,
    @field:NotBlank val fromColumn: String,
    @field:NotBlank val toTable: String,
    @field:NotBlank val toColumn: String
)

data class ForeignKeyRelationshipResponse(
    val id: Long?,
    val workspaceId: Long,
    val fromTable: String,
    val fromColumn: String,
    val toTable: String,
    val toColumn: String,
    val isVirtual: Boolean,
    val discoveredAt: Instant
)

fun ForeignKeyRelationship.toResponse() = ForeignKeyRelationshipResponse(
    id = id,
    workspaceId = workspaceId,
    fromTable = fromTable,
    fromColumn = fromColumn,
    toTable = toTable,
    toColumn = toColumn,
    isVirtual = isVirtual,
    discoveredAt = discoveredAt
)
