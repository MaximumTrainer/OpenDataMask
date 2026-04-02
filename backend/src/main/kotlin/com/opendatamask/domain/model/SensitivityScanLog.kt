package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "sensitivity_scan_logs")
class SensitivityScanLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val workspaceId: Long,

    @Column(nullable = false)
    val startedAt: Instant = Instant.now(),

    var completedAt: Instant? = null,

    @Column(nullable = false)
    var status: String = "RUNNING",

    var tablesScanned: Int = 0,
    var columnsScanned: Int = 0,
    var sensitiveColumnsFound: Int = 0,

    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null
)
