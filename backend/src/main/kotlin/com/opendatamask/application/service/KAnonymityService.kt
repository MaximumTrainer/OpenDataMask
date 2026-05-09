package com.opendatamask.application.service

import com.opendatamask.domain.model.SensitivityType
import com.opendatamask.domain.port.input.dto.KAnonymityReport
import com.opendatamask.domain.port.output.ColumnSensitivityPort
import org.springframework.stereotype.Service

/**
 * Computes a k-anonymity risk score for a workspace by identifying quasi-identifier columns.
 * Quasi-identifiers are sensitive columns that, when combined, can uniquely re-identify an individual.
 * Common quasi-identifiers: age, gender, zip/postal code, date of birth, race/ethnicity.
 */
@Service
class KAnonymityService(
    private val columnSensitivityPort: ColumnSensitivityPort
) {

    companion object {
        // SensitivityTypes that are classic quasi-identifiers per HIPAA Safe Harbor and k-anonymity literature
        private val QUASI_IDENTIFIER_TYPES = setOf(
            SensitivityType.GENDER, SensitivityType.ZIP_CODE, SensitivityType.POSTAL_CODE,
            SensitivityType.BIRTH_DATE, SensitivityType.GPS_COORDINATES
        )
        // Column name keywords that suggest quasi-identifier columns when sensitivityType is UNKNOWN
        private val QUASI_IDENTIFIER_KEYWORDS = setOf(
            "age", "gender", "sex", "zip", "postal", "dob", "birth", "race", "ethnicity",
            "nationality", "marital", "occupation", "education"
        )

        private const val SAFE_K_THRESHOLD = 5
    }

    fun computeKAnonymity(workspaceId: Long): KAnonymityReport {
        val sensitivities = columnSensitivityPort.findByWorkspaceId(workspaceId)

        val quasiIdentifiers = sensitivities
            .filter { col ->
                col.isSensitive && (
                    col.sensitivityType in QUASI_IDENTIFIER_TYPES ||
                    QUASI_IDENTIFIER_KEYWORDS.any { keyword -> col.columnName.lowercase().contains(keyword) }
                )
            }
            .map { "${it.tableName}.${it.columnName}" }
            .distinct()
            .sorted()

        // Estimate k: with N quasi-identifiers, k is roughly 100/2^N.
        // With 0 QIs, k is effectively infinite (no re-identification risk from these columns).
        val estimatedK = when {
            quasiIdentifiers.isEmpty() -> Int.MAX_VALUE
            else -> maxOf(1, (100.0 / Math.pow(2.0, quasiIdentifiers.size.toDouble())).toInt())
        }

        val atRisk = estimatedK < SAFE_K_THRESHOLD

        val recommendation = when {
            quasiIdentifiers.isEmpty() -> "No quasi-identifiers detected. Privacy risk appears low."
            atRisk -> "k-anonymity score is $estimatedK (below safe threshold of $SAFE_K_THRESHOLD). " +
                "Consider generalising or suppressing: ${quasiIdentifiers.take(3).joinToString(", ")}."
            else -> "k-anonymity score is $estimatedK. Monitor as new columns are added."
        }

        return KAnonymityReport(
            workspaceId = workspaceId,
            kValue = estimatedK,
            atRisk = atRisk,
            quasiIdentifiers = quasiIdentifiers,
            recommendation = recommendation
        )
    }
}
