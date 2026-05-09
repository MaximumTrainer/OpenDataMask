package com.opendatamask.domain.port.input.dto

data class HipaaPhiColumnDetail(
    val tableName: String,
    val columnName: String,
    val sensitivityType: String,
    val isMasked: Boolean,
    val appliedGenerator: String?
)

enum class HipaaComplianceStatus { COMPLIANT, NON_COMPLIANT, NOT_DETECTED }

data class HipaaPhiCategory(
    val categoryId: String,
    val displayName: String,
    val description: String,
    val status: HipaaComplianceStatus,
    val affectedColumns: List<HipaaPhiColumnDetail>
)

data class HipaaComplianceReport(
    val workspaceId: Long,
    val overallStatus: HipaaComplianceStatus,
    val compliantCategories: Int,
    val nonCompliantCategories: Int,
    val notDetectedCategories: Int,
    val categories: List<HipaaPhiCategory>
)
