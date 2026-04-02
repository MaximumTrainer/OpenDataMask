package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "column_sensitivities",
    uniqueConstraints = [UniqueConstraint(columnNames = ["workspace_id", "table_name", "column_name"])]
)
class ColumnSensitivity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "workspace_id", nullable = false)
    val workspaceId: Long,

    @Column(name = "table_name", nullable = false)
    val tableName: String,

    @Column(name = "column_name", nullable = false)
    val columnName: String,

    @Column(nullable = false)
    var isSensitive: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var sensitivityType: SensitivityType = SensitivityType.UNKNOWN,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var confidenceLevel: ConfidenceLevel = ConfidenceLevel.LOW,

    @Enumerated(EnumType.STRING)
    var recommendedGeneratorType: GeneratorType? = null,

    @Column(nullable = false)
    val detectedAt: Instant = Instant.now()
)
