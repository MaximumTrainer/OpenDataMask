package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "foreign_key_relationships",
    uniqueConstraints = [UniqueConstraint(columnNames = ["workspace_id", "from_table", "from_column", "to_table", "to_column"])]
)
class ForeignKeyRelationship(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "workspace_id", nullable = false)
    val workspaceId: Long,

    @Column(name = "from_table", nullable = false)
    val fromTable: String,

    @Column(name = "from_column", nullable = false)
    val fromColumn: String,

    @Column(name = "to_table", nullable = false)
    val toTable: String,

    @Column(name = "to_column", nullable = false)
    val toColumn: String,

    @Column(name = "is_virtual", nullable = false)
    val isVirtual: Boolean = false,

    @Column(name = "discovered_at", nullable = false)
    val discoveredAt: Instant = Instant.now()
)
