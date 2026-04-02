package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "inherited_configs")
class InheritedConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val childWorkspaceId: Long,

    @Column(nullable = false)
    val parentWorkspaceId: Long,

    /** "TABLE_CONFIG" or "COLUMN_GENERATOR" */
    @Column(nullable = false)
    val configType: String,

    @Column(nullable = false)
    val tableName: String,

    @Column
    val columnName: String? = null,

    /** The ID of the copied TableConfiguration or ColumnGenerator in the child workspace */
    @Column(nullable = false)
    val inheritedEntityId: Long,

    @Column(nullable = false)
    val inheritedAt: Instant = Instant.now()
)
