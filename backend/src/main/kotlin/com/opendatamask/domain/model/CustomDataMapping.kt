package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class MappingAction {
    MIGRATE_AS_IS, MASK
}

enum class MaskingStrategy {
    FAKE, HASH, NULL, REDACT, PARTIAL_MASK, REGEX
}

@Entity
@Table(
    name = "custom_data_mappings",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_cdm_workspace_connection_table_column",
            columnNames = ["workspace_id", "connection_id", "table_name", "column_name"]
        )
    ]
)
class CustomDataMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "workspace_id", nullable = false)
    var workspaceId: Long,

    @Column(name = "connection_id", nullable = false)
    var connectionId: Long,

    @Column(name = "table_name", nullable = false)
    var tableName: String,

    @Column(name = "column_name", nullable = false)
    var columnName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var action: MappingAction,

    @Enumerated(EnumType.STRING)
    @Column
    var maskingStrategy: MaskingStrategy? = null,

    @Enumerated(EnumType.STRING)
    @Column
    var fakeGeneratorType: GeneratorType? = null,

    @Column(name = "pii_rule_params", columnDefinition = "TEXT")
    var piiRuleParams: String? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
