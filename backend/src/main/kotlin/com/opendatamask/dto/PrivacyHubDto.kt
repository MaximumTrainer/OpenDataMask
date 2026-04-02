package com.opendatamask.dto

import java.time.LocalDateTime

data class PrivacyHubSummary(
    val atRiskCount: Int,
    val protectedCount: Int,
    val notSensitiveCount: Int,
    val tables: List<TableProtectionSummary>,
    val recommendationsCount: Int
)

data class TableProtectionSummary(
    val name: String,
    val atRisk: Int,
    val protected: Int,
    val notSensitive: Int
)

data class PrivacyRecommendation(
    val tableName: String,
    val columnName: String,
    val sensitivityType: String,
    val confidenceLevel: String,
    val recommendedGenerator: String
)
