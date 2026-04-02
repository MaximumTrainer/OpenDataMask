package com.opendatamask.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.opendatamask.domain.model.*
import com.opendatamask.adapter.output.persistence.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class PrivacyReportServiceTest {

    @Mock private lateinit var privacyReportRepository: PrivacyReportRepository
    @Mock private lateinit var columnSensitivityRepository: ColumnSensitivityRepository
    @Mock private lateinit var columnGeneratorRepository: ColumnGeneratorRepository
    @Mock private lateinit var tableConfigurationRepository: TableConfigurationRepository
    @Mock private lateinit var objectMapper: ObjectMapper

    @InjectMocks
    private lateinit var service: PrivacyReportService

    private fun makeSensitivity(tableName: String, columnName: String, isSensitive: Boolean = true) =
        ColumnSensitivity(workspaceId = 1L, tableName = tableName, columnName = columnName).also {
            it.isSensitive = isSensitive
            it.sensitivityType = if (isSensitive) SensitivityType.EMAIL else SensitivityType.UNKNOWN
            it.confidenceLevel = if (isSensitive) ConfidenceLevel.HIGH else ConfidenceLevel.LOW
            it.recommendedGeneratorType = if (isSensitive) GeneratorType.EMAIL else null
        }

    private fun makeReport(id: Long, workspaceId: Long, type: String, jobId: Long? = null) =
        PrivacyReport(id = id, workspaceId = workspaceId, reportType = type, jobId = jobId, reportJson = "{}")

    // ── generateCurrentConfigReport tests ────────────────────────────────

    @Test
    fun `generateCurrentConfigReport persists a CURRENT_CONFIG report`() {
        val sensitivity = makeSensitivity("users", "email")
        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(sensitivity))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(emptyList())
        whenever(objectMapper.writeValueAsString(any())).thenReturn("{\"workspaceId\":1}")
        val expectedReport = makeReport(1L, 1L, "CURRENT_CONFIG")
        whenever(privacyReportRepository.save(any<PrivacyReport>())).thenReturn(expectedReport)

        val result = service.generateCurrentConfigReport(1L)

        assertEquals("CURRENT_CONFIG", result.reportType)
        assertEquals(1L, result.workspaceId)

        val captor = argumentCaptor<PrivacyReport>()
        verify(privacyReportRepository).save(captor.capture())
        assertEquals("CURRENT_CONFIG", captor.firstValue.reportType)
        assertNull(captor.firstValue.jobId)
    }

    @Test
    fun `generateCurrentConfigReport includes expected fields in report data`() {
        val sensitivity = makeSensitivity("users", "email")
        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(sensitivity))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(emptyList())

        val capturedData = argumentCaptor<Map<*, *>>()
        whenever(objectMapper.writeValueAsString(capturedData.capture())).thenReturn("{}")
        whenever(privacyReportRepository.save(any<PrivacyReport>()))
            .thenAnswer { it.arguments[0] as PrivacyReport }

        service.generateCurrentConfigReport(1L)

        val data = capturedData.firstValue
        assertTrue(data.containsKey("workspaceId"))
        assertTrue(data.containsKey("totalColumns"))
        assertTrue(data.containsKey("atRiskCount"))
        assertTrue(data.containsKey("protectedCount"))
        assertTrue(data.containsKey("columns"))
        assertEquals(1, data["totalColumns"])
        assertEquals(1, data["atRiskCount"])
    }

    // ── generateJobReport tests ────────────────────────────────────────────

    @Test
    fun `generateJobReport persists a JOB report with jobId`() {
        val sensitivity = makeSensitivity("users", "email")
        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(sensitivity))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(emptyList())
        whenever(objectMapper.writeValueAsString(any())).thenReturn("{\"jobId\":42}")
        val expectedReport = makeReport(2L, 1L, "JOB", jobId = 42L)
        whenever(privacyReportRepository.save(any<PrivacyReport>())).thenReturn(expectedReport)

        val result = service.generateJobReport(42L, 1L)

        assertEquals("JOB", result.reportType)
        assertEquals(42L, result.jobId)

        val captor = argumentCaptor<PrivacyReport>()
        verify(privacyReportRepository).save(captor.capture())
        assertEquals("JOB", captor.firstValue.reportType)
        assertEquals(42L, captor.firstValue.jobId)
    }

    @Test
    fun `generateJobReport report data contains jobId field`() {
        val sensitivity = makeSensitivity("users", "email")
        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(sensitivity))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(emptyList())

        val capturedData = argumentCaptor<Map<*, *>>()
        whenever(objectMapper.writeValueAsString(capturedData.capture())).thenReturn("{}")
        whenever(privacyReportRepository.save(any<PrivacyReport>()))
            .thenAnswer { it.arguments[0] as PrivacyReport }

        service.generateJobReport(99L, 1L)

        val data = capturedData.firstValue
        assertEquals(99L, data["jobId"])
        assertEquals("JOB", data["reportType"])
    }

    // ── getLatestCurrentReport tests ──────────────────────────────────────

    @Test
    fun `getLatestCurrentReport returns null when no recent reports exist`() {
        whenever(privacyReportRepository.findByWorkspaceIdOrderByGeneratedAtDesc(1L))
            .thenReturn(emptyList())

        val result = service.getLatestCurrentReport(1L)

        assertNull(result)
    }

    @Test
    fun `getLatestCurrentReport returns existing report within time window`() {
        val recent = makeReport(1L, 1L, "CURRENT_CONFIG")
        whenever(privacyReportRepository.findByWorkspaceIdOrderByGeneratedAtDesc(1L))
            .thenReturn(listOf(recent))

        val result = service.getLatestCurrentReport(1L)

        assertNotNull(result)
        assertEquals(1L, result!!.id)
    }

    // ── getJobReport tests ────────────────────────────────────────────────

    @Test
    fun `getJobReport returns null when no report for job`() {
        whenever(privacyReportRepository.findByJobId(42L)).thenReturn(emptyList())

        val result = service.getJobReport(42L)

        assertNull(result)
    }

    @Test
    fun `getJobReport returns first report for job`() {
        val jobReport = makeReport(5L, 1L, "JOB", jobId = 42L)
        whenever(privacyReportRepository.findByJobId(42L)).thenReturn(listOf(jobReport))

        val result = service.getJobReport(42L)

        assertNotNull(result)
        assertEquals(42L, result!!.jobId)
    }
}
