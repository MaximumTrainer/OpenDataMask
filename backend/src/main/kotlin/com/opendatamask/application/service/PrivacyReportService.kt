package com.opendatamask.application.service

import com.opendatamask.domain.port.input.PrivacyReportUseCase

import com.fasterxml.jackson.databind.ObjectMapper
import com.opendatamask.domain.model.PrivacyReport
import com.opendatamask.domain.port.output.ColumnGeneratorPort
import com.opendatamask.domain.port.output.ColumnSensitivityPort
import com.opendatamask.domain.port.output.PrivacyReportPort
import com.opendatamask.domain.port.output.TableConfigurationPort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PrivacyReportService(
    private val privacyReportRepository: PrivacyReportPort,
    private val columnSensitivityRepository: ColumnSensitivityPort,
    private val columnGeneratorRepository: ColumnGeneratorPort,
    private val tableConfigurationRepository: TableConfigurationPort,
    private val objectMapper: ObjectMapper
) : PrivacyReportUseCase {

    override fun generateCurrentConfigReport(workspaceId: Long): PrivacyReport {
        val reportData = buildReportData(workspaceId, jobId = null)
        val reportJson = objectMapper.writeValueAsString(reportData)
        val report = PrivacyReport(
            workspaceId = workspaceId,
            reportType = "CURRENT_CONFIG",
            reportJson = reportJson
        )
        return privacyReportRepository.save(report)
    }

    override fun generateJobReport(jobId: Long, workspaceId: Long): PrivacyReport {
        val reportData = buildReportData(workspaceId, jobId = jobId)
        val reportJson = objectMapper.writeValueAsString(reportData)
        val report = PrivacyReport(
            workspaceId = workspaceId,
            jobId = jobId,
            reportType = "JOB",
            reportJson = reportJson
        )
        return privacyReportRepository.save(report)
    }

    override fun getLatestCurrentReport(workspaceId: Long, withinHours: Long): PrivacyReport? {
        val cutoff = LocalDateTime.now().minusHours(withinHours)
        return privacyReportRepository
            .findByWorkspaceIdOrderByGeneratedAtDesc(workspaceId)
            .firstOrNull { it.reportType == "CURRENT_CONFIG" && it.generatedAt.isAfter(cutoff) }
    }

    override fun getJobReport(jobId: Long): PrivacyReport? =
        privacyReportRepository.findByJobId(jobId).firstOrNull()

    override fun getReportsForWorkspace(workspaceId: Long): List<PrivacyReport> =
        privacyReportRepository.findByWorkspaceIdOrderByGeneratedAtDesc(workspaceId)

    private fun buildReportData(workspaceId: Long, jobId: Long?): Map<String, Any?> {
        val sensitivities = columnSensitivityRepository.findByWorkspaceId(workspaceId)
        val tableConfigs = tableConfigurationRepository.findByWorkspaceId(workspaceId)

        val generatorMap = mutableMapOf<Pair<String, String>, String>()
        for (tc in tableConfigs) {
            for (gen in columnGeneratorRepository.findByTableConfigurationId(tc.id)) {
                generatorMap[Pair(tc.tableName, gen.columnName)] = gen.generatorType.name
            }
        }

        val columns = sensitivities.map { col ->
            val generator = generatorMap[Pair(col.tableName, col.columnName)]
            val status = when {
                generator != null -> "PROTECTED"
                col.isSensitive -> "AT_RISK"
                else -> "NOT_SENSITIVE"
            }
            mapOf(
                "tableName" to col.tableName,
                "columnName" to col.columnName,
                "isSensitive" to col.isSensitive,
                "sensitivityType" to col.sensitivityType.name,
                "confidenceLevel" to col.confidenceLevel.name,
                "recommendedGenerator" to col.recommendedGeneratorType?.name,
                "appliedGenerator" to generator,
                "status" to status
            )
        }

        return mapOf(
            "workspaceId" to workspaceId,
            "jobId" to jobId,
            "generatedAt" to LocalDateTime.now().toString(),
            "reportType" to if (jobId != null) "JOB" else "CURRENT_CONFIG",
            "totalColumns" to sensitivities.size,
            "atRiskCount" to columns.count { it["status"] == "AT_RISK" },
            "protectedCount" to columns.count { it["status"] == "PROTECTED" },
            "notSensitiveCount" to columns.count { it["status"] == "NOT_SENSITIVE" },
            "columns" to columns
        )
    }
}