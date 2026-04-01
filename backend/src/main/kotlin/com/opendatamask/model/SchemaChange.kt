package com.opendatamask.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class SchemaChangeType {
    NEW_TABLE, DROPPED_TABLE, NEW_COLUMN, DROPPED_COLUMN, TYPE_CHANGED, NULLABILITY_CHANGED
}

enum class SchemaChangeStatus {
    UNRESOLVED, RESOLVED, DISMISSED
}

@Entity
@Table(name = "schema_changes")
class SchemaChange(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var changeType: SchemaChangeType,

    @Column(nullable = false)
    var tableName: String,

    @Column
    var columnName: String? = null,

    @Column
    var oldValue: String? = null,

    @Column
    var newValue: String? = null,

    @Column(nullable = false)
    var detectedAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SchemaChangeStatus = SchemaChangeStatus.UNRESOLVED
)
