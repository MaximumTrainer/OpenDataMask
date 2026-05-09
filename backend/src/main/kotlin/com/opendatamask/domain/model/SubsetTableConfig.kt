package com.opendatamask.domain.model

import jakarta.persistence.*

enum class SubsetLimitType { PERCENTAGE, ROW_COUNT, ALL }

@Entity
@Table(
    name = "subset_table_configs",
    uniqueConstraints = [UniqueConstraint(columnNames = ["workspace_id", "table_name"])]
)
class SubsetTableConfig(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "workspace_id", nullable = false)
    val workspaceId: Long,

    @Column(name = "table_name", nullable = false)
    val tableName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false)
    var limitType: SubsetLimitType = SubsetLimitType.PERCENTAGE,

    @Column(name = "limit_value", nullable = false)
    var limitValue: Int = 10,

    @Column(name = "is_target_table", nullable = false)
    var isTargetTable: Boolean = false,

    @Column(name = "is_lookup_table", nullable = false)
    var isLookupTable: Boolean = false,

    /**
     * Optional WHERE clause used as the seed filter when this is a target (root) table.
     * Allows multiple root tables to be seeded with different filters simultaneously.
     * e.g. "created_at > '2024-01-01'" to seed only recent users.
     */
    @Column(name = "seed_filter", length = 2048)
    var seedFilter: String? = null
)
