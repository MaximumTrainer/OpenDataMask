package com.opendatamask.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "sensitivity_scan_log_entries")
class SensitivityScanLogEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val scanLogId: Long,

    @Column(nullable = false)
    val tableName: String,

    @Column(nullable = false)
    val columnName: String,

    val detectedType: String? = null,
    val confidenceLevel: String? = null,
    val recommendedGenerator: String? = null,

    @Column(nullable = false)
    val scannedAt: LocalDateTime = LocalDateTime.now()
)
