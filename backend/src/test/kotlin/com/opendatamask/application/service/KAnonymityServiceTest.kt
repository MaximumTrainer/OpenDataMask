package com.opendatamask.application.service

import com.opendatamask.domain.model.ColumnSensitivity
import com.opendatamask.domain.model.ConfidenceLevel
import com.opendatamask.domain.model.SensitivityType
import com.opendatamask.domain.port.output.ColumnSensitivityPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class KAnonymityServiceTest {

    private val columnSensitivityPort: ColumnSensitivityPort = mock()
    private val service = KAnonymityService(columnSensitivityPort)

    private fun makeColumn(
        tableName: String,
        columnName: String,
        isSensitive: Boolean = true,
        sensitivityType: SensitivityType = SensitivityType.UNKNOWN
    ) = ColumnSensitivity(
        workspaceId = 1L,
        tableName = tableName,
        columnName = columnName,
        isSensitive = isSensitive,
        sensitivityType = sensitivityType,
        confidenceLevel = ConfidenceLevel.HIGH
    )

    @Test
    fun `returns safe score when no columns found`() {
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(emptyList())

        val report = service.computeKAnonymity(1L)

        assertEquals(Int.MAX_VALUE, report.kValue)
        assertFalse(report.atRisk)
        assertTrue(report.quasiIdentifiers.isEmpty())
    }

    @Test
    fun `detects GENDER and ZIP_CODE as quasi-identifiers`() {
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(
            makeColumn("users", "gender", sensitivityType = SensitivityType.GENDER),
            makeColumn("users", "zip_code", sensitivityType = SensitivityType.ZIP_CODE),
            makeColumn("users", "email", sensitivityType = SensitivityType.EMAIL)
        ))

        val report = service.computeKAnonymity(1L)

        assertTrue(report.quasiIdentifiers.contains("users.gender"))
        assertTrue(report.quasiIdentifiers.contains("users.zip_code"))
        assertFalse(report.quasiIdentifiers.contains("users.email"))
    }

    @Test
    fun `detects quasi-identifier by column name keyword when sensitivityType is UNKNOWN`() {
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(
            makeColumn("patients", "dob", sensitivityType = SensitivityType.UNKNOWN),
            makeColumn("patients", "age_bucket", sensitivityType = SensitivityType.UNKNOWN)
        ))

        val report = service.computeKAnonymity(1L)

        assertTrue(report.quasiIdentifiers.contains("patients.dob"))
        assertTrue(report.quasiIdentifiers.contains("patients.age_bucket"))
    }

    @Test
    fun `marks atRisk true when k is below threshold`() {
        // 4 quasi-identifiers → k = 100/16 = 6 → NOT at risk. Use 5 to get k=100/32=3 → at risk
        val columns = (1..5).map { i ->
            makeColumn("users", "qi_col_$i", sensitivityType = SensitivityType.ZIP_CODE)
        }
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(columns)

        val report = service.computeKAnonymity(1L)

        assertTrue(report.atRisk)
        assertTrue(report.kValue < 5)
    }

    @Test
    fun `does not include non-sensitive columns as quasi-identifiers`() {
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(
            makeColumn("users", "gender", isSensitive = false, sensitivityType = SensitivityType.GENDER)
        ))

        val report = service.computeKAnonymity(1L)

        assertTrue(report.quasiIdentifiers.isEmpty())
        assertEquals(Int.MAX_VALUE, report.kValue)
    }

    @Test
    fun `quasi-identifiers are returned sorted`() {
        whenever(columnSensitivityPort.findByWorkspaceId(1L)).thenReturn(listOf(
            makeColumn("users", "zip_code", sensitivityType = SensitivityType.ZIP_CODE),
            makeColumn("patients", "dob", sensitivityType = SensitivityType.BIRTH_DATE),
            makeColumn("users", "gender", sensitivityType = SensitivityType.GENDER)
        ))

        val report = service.computeKAnonymity(1L)

        assertEquals(report.quasiIdentifiers, report.quasiIdentifiers.sorted())
    }
}
