package com.opendatamask.application.service

import com.opendatamask.domain.model.*
import com.opendatamask.domain.port.input.dto.HipaaComplianceStatus
import com.opendatamask.domain.port.output.ColumnGeneratorPort
import com.opendatamask.domain.port.output.ColumnSensitivityPort
import com.opendatamask.domain.port.output.TableConfigurationPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class HipaaComplianceServiceTest {

    @Mock private lateinit var columnSensitivityPort: ColumnSensitivityPort
    @Mock private lateinit var columnGeneratorPort: ColumnGeneratorPort
    @Mock private lateinit var tableConfigurationPort: TableConfigurationPort
    @InjectMocks private lateinit var service: HipaaComplianceService

    private fun sensitivity(tableName: String, columnName: String, type: SensitivityType) = ColumnSensitivity(
        id = null,
        workspaceId = 1L,
        tableName = tableName,
        columnName = columnName,
        isSensitive = true,
        sensitivityType = type,
        confidenceLevel = ConfidenceLevel.FULL
    )

    private fun tableConfig(id: Long, tableName: String) = TableConfiguration(
        id = id,
        workspaceId = 1L,
        tableName = tableName
    )

    private fun generator(tableConfigId: Long, columnName: String, type: GeneratorType) = ColumnGenerator(
        tableConfigurationId = tableConfigId,
        columnName = columnName,
        generatorType = type
    )

    @Test
    fun `should return NOT_DETECTED for all categories when workspace has no sensitive columns`() {
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(emptyList())
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(emptyList())

        val report = service.getComplianceReport(1L)

        assertEquals(HipaaComplianceStatus.NOT_DETECTED, report.overallStatus)
        assertEquals(0, report.compliantCategories)
        assertEquals(0, report.nonCompliantCategories)
        assertTrue(report.categories.all { it.status == HipaaComplianceStatus.NOT_DETECTED })
    }

    @Test
    fun `should return NON_COMPLIANT when PHI column has no generator assigned`() {
        val emailCol = sensitivity("users", "email", SensitivityType.EMAIL)
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(emailCol))
        val tc = tableConfig(10L, "users")
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(listOf(tc))
        whenever(columnGeneratorPort.findByTableConfigurationId(10L)).thenReturn(emptyList())

        val report = service.getComplianceReport(1L)

        assertEquals(HipaaComplianceStatus.NON_COMPLIANT, report.overallStatus)
        assertEquals(1, report.nonCompliantCategories)
        val emailCategory = report.categories.first { it.categoryId == "PHI_5_EMAIL" }
        assertEquals(HipaaComplianceStatus.NON_COMPLIANT, emailCategory.status)
        assertFalse(emailCategory.affectedColumns.first().isMasked)
    }

    @Test
    fun `should return COMPLIANT when PHI column has a masking generator assigned`() {
        val ssnCol = sensitivity("users", "ssn", SensitivityType.SSN)
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(ssnCol))
        val tc = tableConfig(10L, "users")
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(listOf(tc))
        whenever(columnGeneratorPort.findByTableConfigurationId(10L))
            .thenReturn(listOf(generator(10L, "ssn", GeneratorType.SSN)))

        val report = service.getComplianceReport(1L)

        val ssnCategory = report.categories.first { it.categoryId == "PHI_6_SSN" }
        assertEquals(HipaaComplianceStatus.COMPLIANT, ssnCategory.status)
        assertTrue(ssnCategory.affectedColumns.first().isMasked)
        assertEquals("SSN", ssnCategory.affectedColumns.first().appliedGenerator)
    }

    @Test
    fun `should mark NON_COMPLIANT when some PHI columns are masked and some are not`() {
        val firstNameCol = sensitivity("users", "first_name", SensitivityType.FIRST_NAME)
        val lastNameCol = sensitivity("users", "last_name", SensitivityType.LAST_NAME)
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(firstNameCol, lastNameCol))
        val tc = tableConfig(10L, "users")
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(listOf(tc))
        // Only first_name is masked
        whenever(columnGeneratorPort.findByTableConfigurationId(10L))
            .thenReturn(listOf(generator(10L, "first_name", GeneratorType.FIRST_NAME)))

        val report = service.getComplianceReport(1L)

        val namesCategory = report.categories.first { it.categoryId == "PHI_1_NAMES" }
        assertEquals(HipaaComplianceStatus.NON_COMPLIANT, namesCategory.status)
        assertEquals(2, namesCategory.affectedColumns.size)
        assertTrue(namesCategory.affectedColumns.first { it.columnName == "first_name" }.isMasked)
        assertFalse(namesCategory.affectedColumns.first { it.columnName == "last_name" }.isMasked)
    }

    @Test
    fun `should not include non-sensitive columns in HIPAA report`() {
        val notSensitive = ColumnSensitivity(
            id = null, workspaceId = 1L, tableName = "users", columnName = "created_at",
            isSensitive = false, sensitivityType = SensitivityType.UNKNOWN, confidenceLevel = ConfidenceLevel.LOW
        )
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(notSensitive))
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(emptyList())

        val report = service.getComplianceReport(1L)

        assertEquals(HipaaComplianceStatus.NOT_DETECTED, report.overallStatus)
        assertTrue(report.categories.all { it.affectedColumns.isEmpty() })
    }

    @Test
    fun `should correctly count compliant and non-compliant categories`() {
        val emailCol = sensitivity("users", "email", SensitivityType.EMAIL)
        val ssnCol = sensitivity("users", "ssn", SensitivityType.SSN)
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(emailCol, ssnCol))
        val tc = tableConfig(10L, "users")
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(listOf(tc))
        // SSN masked, email not masked
        whenever(columnGeneratorPort.findByTableConfigurationId(10L))
            .thenReturn(listOf(generator(10L, "ssn", GeneratorType.SSN)))

        val report = service.getComplianceReport(1L)

        assertEquals(1, report.compliantCategories)
        assertEquals(1, report.nonCompliantCategories)
        assertEquals(HipaaComplianceStatus.NON_COMPLIANT, report.overallStatus)
    }

    @Test
    fun `should handle multiple tables in a single workspace`() {
        val patientNameCol = sensitivity("patients", "name", SensitivityType.FULL_NAME)
        val recordCol = sensitivity("records", "medical_id", SensitivityType.MEDICAL_RECORD_NUMBER)
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(patientNameCol, recordCol))

        val tc1 = tableConfig(10L, "patients")
        val tc2 = tableConfig(20L, "records")
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(listOf(tc1, tc2))
        whenever(columnGeneratorPort.findByTableConfigurationId(10L))
            .thenReturn(listOf(generator(10L, "name", GeneratorType.FULL_NAME)))
        whenever(columnGeneratorPort.findByTableConfigurationId(20L))
            .thenReturn(listOf(generator(20L, "medical_id", GeneratorType.MEDICAL_RECORD_NUMBER)))

        val report = service.getComplianceReport(1L)

        val namesCategory = report.categories.first { it.categoryId == "PHI_1_NAMES" }
        val medCategory = report.categories.first { it.categoryId == "PHI_7_MEDICAL_RECORD" }
        assertEquals(HipaaComplianceStatus.COMPLIANT, namesCategory.status)
        assertEquals(HipaaComplianceStatus.COMPLIANT, medCategory.status)
        assertEquals(HipaaComplianceStatus.COMPLIANT, report.overallStatus)
    }
}
