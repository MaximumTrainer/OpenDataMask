package com.opendatamask.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "privacy_reports")
class PrivacyReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val workspaceId: Long,

    val jobId: Long? = null,

    @Column(nullable = false)
    val generatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val reportType: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val reportJson: String
)
