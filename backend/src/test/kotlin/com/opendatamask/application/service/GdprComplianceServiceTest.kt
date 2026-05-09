package com.opendatamask.application.service

import com.opendatamask.domain.model.*
import com.opendatamask.domain.port.output.ColumnGeneratorPort
import com.opendatamask.domain.port.output.ColumnSensitivityPort
import com.opendatamask.domain.port.output.TableConfigurationPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GdprComplianceServiceTest {

    private val columnSensitivityPort: ColumnSensitivityPort = mock()
    private val tableConfigurationPort: TableConfigurationPort = mock()
    private val columnGeneratorPort: ColumnGeneratorPort = mock()
    private val service = GdprComplianceService(columnSensitivityPort, tableConfigurationPort, columnGeneratorPort)

    private fun makeTableConfig(id: Long, tableName: String) = TableConfiguration(
        id = id, workspaceId = 1L, tableName = tableName
    )

    private fun makeSensitivity(tableName: String, columnName: String, sensitive: Boolean = true) = ColumnSensitivity(
        workspaceId = 1L,
        tableName = tableName,
        columnName = columnName,
        isSensitive = sensitive,
        sensitivityType = SensitivityType.EMAIL,
        confidenceLevel = ConfidenceLevel.HIGH
    )

    private fun makeGenerator(tableConfigId: Long, columnName: String) = ColumnGenerator(
        tableConfigurationId = tableConfigId,
        columnName = columnName,
        generatorType = GeneratorType.EMAIL
    )

    @Test
    fun `report is compliant when all sensitive columns have generators`() {
        val tableConfig = makeTableConfig(1L, "users")
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(
            makeSensitivity("users", "email")
        ))
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(listOf(tableConfig))
        whenever(columnGeneratorPort.findByTableConfigurationId(1L)).thenReturn(listOf(
            makeGenerator(1L, "email")
        ))

        val report = service.generateReport(1L)

        assertTrue(report.overallCompliant)
        assertTrue(report.personalDataColumns.all { it.isProtected })
    }

    @Test
    fun `report is not compliant when sensitive columns lack generators`() {
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(
            makeSensitivity("users", "email"),
            makeSensitivity("users", "phone")
        ))
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(emptyList())

        val report = service.generateReport(1L)

        assertFalse(report.overallCompliant)
        assertEquals(2, report.personalDataColumns.count { !it.isProtected })
    }

    @Test
    fun `report contains six GDPR Article 5 principle checks`() {
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(emptyList())
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(emptyList())

        val report = service.generateReport(1L)

        assertEquals(6, report.principleChecks.size)
    }

    @Test
    fun `report excludes non-sensitive columns from personal data list`() {
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(
            makeSensitivity("users", "email", sensitive = true),
            makeSensitivity("users", "id", sensitive = false)
        ))
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(emptyList())

        val report = service.generateReport(1L)

        assertEquals(1, report.personalDataColumns.size)
        assertEquals("email", report.personalDataColumns.first().columnName)
    }

    @Test
    fun `personal data columns are sorted by table and column name`() {
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(
            makeSensitivity("users", "phone"),
            makeSensitivity("users", "email"),
            makeSensitivity("accounts", "ssn")
        ))
        whenever(tableConfigurationPort.findByWorkspaceId(1L)).thenReturn(emptyList())

        val report = service.generateReport(1L)

        val keys = report.personalDataColumns.map { "${it.tableName}.${it.columnName}" }
        assertEquals(keys, keys.sorted())
    }
}
