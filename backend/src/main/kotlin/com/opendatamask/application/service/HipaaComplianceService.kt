package com.opendatamask.application.service

import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.model.SensitivityType
import com.opendatamask.domain.port.input.HipaaComplianceUseCase
import com.opendatamask.domain.port.input.dto.HipaaComplianceReport
import com.opendatamask.domain.port.input.dto.HipaaComplianceStatus
import com.opendatamask.domain.port.input.dto.HipaaPhiCategory
import com.opendatamask.domain.port.input.dto.HipaaPhiColumnDetail
import com.opendatamask.domain.port.output.ColumnGeneratorPort
import com.opendatamask.domain.port.output.ColumnSensitivityPort
import com.opendatamask.domain.port.output.TableConfigurationPort
import org.springframework.stereotype.Service

private data class PhiCategoryDefinition(
    val categoryId: String,
    val displayName: String,
    val description: String,
    val sensitivityTypes: Set<SensitivityType>
)

private val HIPAA_SAFE_HARBOR_CATEGORIES = listOf(
    PhiCategoryDefinition("PHI_1_NAMES", "Names",
        "Full or partial names (first name, last name, full name) of individuals.",
        setOf(SensitivityType.FIRST_NAME, SensitivityType.LAST_NAME, SensitivityType.FULL_NAME)),
    PhiCategoryDefinition("PHI_2_GEO", "Geographic Data",
        "Street address, city, ZIP code, GPS coordinates — any subdivision smaller than a state.",
        setOf(SensitivityType.STREET_ADDRESS, SensitivityType.ZIP_CODE, SensitivityType.CITY,
            SensitivityType.GPS_COORDINATES, SensitivityType.POSTAL_CODE)),
    PhiCategoryDefinition("PHI_3_DATES", "Dates (except year)",
        "Birth dates and other dates directly related to an individual.",
        setOf(SensitivityType.BIRTH_DATE)),
    PhiCategoryDefinition("PHI_4_PHONE", "Telephone Numbers",
        "Telephone and fax numbers that identify an individual.",
        setOf(SensitivityType.PHONE)),
    PhiCategoryDefinition("PHI_5_EMAIL", "Email Addresses",
        "Email addresses that identify an individual.",
        setOf(SensitivityType.EMAIL)),
    PhiCategoryDefinition("PHI_6_SSN", "Social Security Numbers",
        "US Social Security Numbers or equivalent national identifiers.",
        setOf(SensitivityType.SSN)),
    PhiCategoryDefinition("PHI_7_MEDICAL_RECORD", "Medical Record Numbers",
        "Numbers assigned to individuals within healthcare systems.",
        setOf(SensitivityType.MEDICAL_RECORD_NUMBER)),
    PhiCategoryDefinition("PHI_8_HEALTH_PLAN", "Health Plan Beneficiary Numbers",
        "Health plan subscriber or beneficiary identifiers.",
        setOf(SensitivityType.HEALTH_PLAN_NUMBER)),
    PhiCategoryDefinition("PHI_9_ACCOUNT", "Account Numbers",
        "Financial account numbers and other unique account identifiers.",
        setOf(SensitivityType.ACCOUNT_NUMBER, SensitivityType.IBAN)),
    PhiCategoryDefinition("PHI_10_LICENSE", "Certificate and License Numbers",
        "Driver's licenses, passports, and other government-issued identifiers.",
        setOf(SensitivityType.DRIVERS_LICENSE, SensitivityType.PASSPORT_NUMBER)),
    PhiCategoryDefinition("PHI_11_VIN", "Vehicle Identifiers",
        "Vehicle identification numbers (VINs) and license plate numbers.",
        setOf(SensitivityType.VIN, SensitivityType.LICENSE_PLATE)),
    PhiCategoryDefinition("PHI_12_URL", "Web URLs",
        "Web Universal Resource Locators that identify individuals.",
        setOf(SensitivityType.URL)),
    PhiCategoryDefinition("PHI_13_IP", "IP Addresses",
        "IPv4 and IPv6 addresses that may identify individuals.",
        setOf(SensitivityType.IP_ADDRESS, SensitivityType.IPV6_ADDRESS)),
    PhiCategoryDefinition("PHI_14_BIOMETRIC", "Biometric Identifiers",
        "Fingerprints, voice prints, retina scans, and other biometric identifiers.",
        setOf(SensitivityType.BIOMETRIC)),
    PhiCategoryDefinition("PHI_15_CREDIT_CARD", "Financial Card Numbers",
        "Credit and debit card numbers that directly identify individuals.",
        setOf(SensitivityType.CREDIT_CARD))
)

/** Generator types that indicate a column is actively masked (not a passthrough). */
private val MASKING_GENERATOR_TYPES: Set<GeneratorType> = GeneratorType.entries
    .filter { it != GeneratorType.CUSTOM }
    .toSet()

@Service
class HipaaComplianceService(
    private val columnSensitivityRepository: ColumnSensitivityPort,
    private val columnGeneratorRepository: ColumnGeneratorPort,
    private val tableConfigurationRepository: TableConfigurationPort
) : HipaaComplianceUseCase {

    override fun getComplianceReport(workspaceId: Long): HipaaComplianceReport {
        val sensitivities = columnSensitivityRepository.findByWorkspaceId(workspaceId)
            .filter { it.isSensitive }

        val tableConfigs = tableConfigurationRepository.findByWorkspaceId(workspaceId)

        val generatorByColumn: Map<Pair<String, String>, GeneratorType?> = tableConfigs.flatMap { tc ->
            columnGeneratorRepository.findByTableConfigurationId(tc.id)
                .map { gen -> Pair(tc.tableName, gen.columnName) to gen.generatorType }
        }.toMap()

        val categories = HIPAA_SAFE_HARBOR_CATEGORIES.map { def ->
            val matchingColumns = sensitivities.filter { it.sensitivityType in def.sensitivityTypes }
            if (matchingColumns.isEmpty()) {
                HipaaPhiCategory(
                    categoryId = def.categoryId,
                    displayName = def.displayName,
                    description = def.description,
                    status = HipaaComplianceStatus.NOT_DETECTED,
                    affectedColumns = emptyList()
                )
            } else {
                val columnDetails = matchingColumns.map { col ->
                    val generatorType = generatorByColumn[Pair(col.tableName, col.columnName)]
                    val isMasked = generatorType != null && generatorType in MASKING_GENERATOR_TYPES
                    HipaaPhiColumnDetail(
                        tableName = col.tableName,
                        columnName = col.columnName,
                        sensitivityType = col.sensitivityType.name,
                        isMasked = isMasked,
                        appliedGenerator = generatorType?.name
                    )
                }
                val allMasked = columnDetails.all { it.isMasked }
                HipaaPhiCategory(
                    categoryId = def.categoryId,
                    displayName = def.displayName,
                    description = def.description,
                    status = if (allMasked) HipaaComplianceStatus.COMPLIANT else HipaaComplianceStatus.NON_COMPLIANT,
                    affectedColumns = columnDetails
                )
            }
        }

        val compliant = categories.count { it.status == HipaaComplianceStatus.COMPLIANT }
        val nonCompliant = categories.count { it.status == HipaaComplianceStatus.NON_COMPLIANT }
        val notDetected = categories.count { it.status == HipaaComplianceStatus.NOT_DETECTED }
        val overall = when {
            nonCompliant > 0 -> HipaaComplianceStatus.NON_COMPLIANT
            compliant > 0 -> HipaaComplianceStatus.COMPLIANT
            else -> HipaaComplianceStatus.NOT_DETECTED
        }

        return HipaaComplianceReport(
            workspaceId = workspaceId,
            overallStatus = overall,
            compliantCategories = compliant,
            nonCompliantCategories = nonCompliant,
            notDetectedCategories = notDetected,
            categories = categories
        )
    }
}
