package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class TableMode {
    PASSTHROUGH, MASK, GENERATE, SUBSET, UPSERT, SKIP
}

@Entity
@Table(name = "table_configurations")
class TableConfiguration(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Column(nullable = false)
    var tableName: String,

    @Column
    var schemaName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var mode: TableMode = TableMode.PASSTHROUGH,

    @Column
    var rowLimit: Long? = null,

    @Column(length = 4096)
    var whereClause: String? = null,

    // Comma-separated list of column names to include in the extraction (null/empty = all columns).
    @Column(length = 2048)
    var selectedAttributes: String? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
    }
}
