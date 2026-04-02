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
    var isLookupTable: Boolean = false
)
