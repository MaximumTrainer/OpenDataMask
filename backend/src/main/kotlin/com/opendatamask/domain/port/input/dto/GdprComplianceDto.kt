package com.opendatamask.domain.port.input.dto

data class GdprComplianceReport(
    val workspaceId: Long,
    val generatedAt: java.time.Instant,
    val principleChecks: List<GdprPrincipleCheck>,
    val personalDataColumns: List<PersonalDataColumnEntry>,
    val overallCompliant: Boolean
)

data class GdprPrincipleCheck(
    val principle: String,
    val description: String,
    val compliant: Boolean,
    val detail: String
)

data class PersonalDataColumnEntry(
    val tableName: String,
    val columnName: String,
    val sensitivityType: String,
    val maskingStrategyApplied: String?,
    val isProtected: Boolean
)
