package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "schema_snapshots")
class SchemaSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Column(nullable = false)
    var snapshotAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, columnDefinition = "TEXT")
    var schemaJson: String = "{}"
)
