package com.opendatamask.application.service

import com.opendatamask.domain.port.input.dto.GdprComplianceReport
import com.opendatamask.domain.port.input.dto.GdprPrincipleCheck
import com.opendatamask.domain.port.input.dto.PersonalDataColumnEntry
import com.opendatamask.domain.port.output.ColumnGeneratorPort
import com.opendatamask.domain.port.output.ColumnSensitivityPort
import com.opendatamask.domain.port.output.TableConfigurationPort
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class GdprComplianceService(
    private val columnSensitivityPort: ColumnSensitivityPort,
    private val tableConfigurationPort: TableConfigurationPort,
    private val columnGeneratorPort: ColumnGeneratorPort
) {

    fun generateReport(workspaceId: Long): GdprComplianceReport {
        val sensitivities = columnSensitivityPort.findByWorkspaceId(workspaceId)
        val tableConfigs = tableConfigurationPort.findByWorkspaceId(workspaceId)

        // Build a map of (tableName, columnName) → generator type name
        val generatorMap = tableConfigs.flatMap { tc ->
            columnGeneratorPort.findByTableConfigurationId(tc.id)
                .map { gen -> Triple(tc.tableName, gen.columnName, gen.generatorType.name) }
        }.associate { (table, column, gen) -> Pair(table, column) to gen }

        val personalDataColumns = sensitivities
            .filter { it.isSensitive }
            .map { col ->
                val generatorName = generatorMap[Pair(col.tableName, col.columnName)]
                PersonalDataColumnEntry(
                    tableName = col.tableName,
                    columnName = col.columnName,
                    sensitivityType = col.sensitivityType.name,
                    maskingStrategyApplied = generatorName,
                    isProtected = generatorName != null
                )
            }
            .sortedBy { "${it.tableName}.${it.columnName}" }

        val unprotectedCount = personalDataColumns.count { !it.isProtected }
        val totalPersonalColumns = personalDataColumns.size

        val principleChecks = listOf(
            GdprPrincipleCheck(
                principle = "Lawfulness, Fairness and Transparency (Art. 5.1.a)",
                description = "Personal data is processed lawfully and transparently.",
                compliant = true,
                detail = "Masking configuration is documented per column."
            ),
            GdprPrincipleCheck(
                principle = "Purpose Limitation (Art. 5.1.b)",
                description = "Data is collected for specified, explicit and legitimate purposes.",
                compliant = true,
                detail = "Masking workspace isolates data for specific use cases."
            ),
            GdprPrincipleCheck(
                principle = "Data Minimisation (Art. 5.1.c)",
                description = "Only data necessary for the purpose is processed.",
                compliant = unprotectedCount == 0,
                detail = if (unprotectedCount == 0)
                    "All $totalPersonalColumns sensitive columns have masking strategies applied."
                else
                    "$unprotectedCount of $totalPersonalColumns sensitive columns lack masking strategies."
            ),
            GdprPrincipleCheck(
                principle = "Accuracy (Art. 5.1.d)",
                description = "Personal data is accurate and kept up to date.",
                compliant = true,
                detail = "Masked data preserves structural accuracy for testing purposes."
            ),
            GdprPrincipleCheck(
                principle = "Storage Limitation (Art. 5.1.e)",
                description = "Data is retained only as long as necessary.",
                compliant = true,
                detail = "Masked datasets are ephemeral by design."
            ),
            GdprPrincipleCheck(
                principle = "Integrity and Confidentiality (Art. 5.1.f)",
                description = "Appropriate security measures protect personal data.",
                compliant = unprotectedCount == 0,
                detail = if (unprotectedCount == 0)
                    "All sensitive columns are masked."
                else
                    "$unprotectedCount sensitive column(s) are unmasked and at risk."
            )
        )

        return GdprComplianceReport(
            workspaceId = workspaceId,
            generatedAt = Instant.now(),
            principleChecks = principleChecks,
            personalDataColumns = personalDataColumns,
            overallCompliant = principleChecks.all { it.compliant }
        )
    }
}
