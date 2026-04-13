package com.opendatamask.application.service

import com.opendatamask.domain.port.input.dto.PrivacyHubSummary
import com.opendatamask.domain.model.*
import com.opendatamask.adapter.output.persistence.*
import com.opendatamask.domain.port.output.GeneratorPresetPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class PrivacyHubServiceTest {

    @Mock private lateinit var columnSensitivityRepository: ColumnSensitivityRepository
    @Mock private lateinit var columnGeneratorRepository: ColumnGeneratorRepository
    @Mock private lateinit var tableConfigurationRepository: TableConfigurationRepository
    @Mock private lateinit var generatorPresetRepository: GeneratorPresetPort

    @InjectMocks
    private lateinit var service: PrivacyHubService

    private fun makeSensitivity(
        tableName: String,
        columnName: String,
        isSensitive: Boolean = true,
        type: SensitivityType = SensitivityType.EMAIL,
        confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
        recommendedType: GeneratorType? = GeneratorType.EMAIL
    ) = ColumnSensitivity(
        workspaceId = 1L,
        tableName = tableName,
        columnName = columnName
    ).also {
        it.isSensitive = isSensitive
        it.sensitivityType = type
        it.confidenceLevel = confidence
        it.recommendedGeneratorType = recommendedType
    }

    private fun makeTableConfig(id: Long, tableName: String) =
        TableConfiguration(id = id, workspaceId = 1L, tableName = tableName)

    private fun makeGenerator(tableConfigId: Long, columnName: String, type: GeneratorType = GeneratorType.EMAIL) =
        ColumnGenerator(tableConfigurationId = tableConfigId, columnName = columnName, generatorType = type)

    // ── getSummary tests ──────────────────────────────────────────────────

    @Test
    fun `getSummary counts AT_RISK when sensitive column has no generator`() {
        val sensitivity = makeSensitivity("users", "email")
        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(sensitivity))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(emptyList())

        val summary = service.getSummary(1L)

        assertEquals(1, summary.atRiskCount)
        assertEquals(0, summary.protectedCount)
        assertEquals(0, summary.notSensitiveCount)
        assertEquals(1, summary.recommendationsCount)
    }

    @Test
    fun `getSummary counts PROTECTED when column has a generator`() {
        val sensitivity = makeSensitivity("users", "email")
        val tc = makeTableConfig(10L, "users")
        val generator = makeGenerator(10L, "email")

        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(sensitivity))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(listOf(tc))
        whenever(columnGeneratorRepository.findByTableConfigurationId(10L)).thenReturn(listOf(generator))

        val summary = service.getSummary(1L)

        assertEquals(0, summary.atRiskCount)
        assertEquals(1, summary.protectedCount)
        assertEquals(0, summary.notSensitiveCount)
        assertEquals(0, summary.recommendationsCount)
    }

    @Test
    fun `getSummary counts NOT_SENSITIVE when column is not sensitive and has no generator`() {
        val sensitivity = makeSensitivity("orders", "total", isSensitive = false, type = SensitivityType.UNKNOWN, confidence = ConfidenceLevel.LOW, recommendedType = null)

        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(sensitivity))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(emptyList())

        val summary = service.getSummary(1L)

        assertEquals(0, summary.atRiskCount)
        assertEquals(0, summary.protectedCount)
        assertEquals(1, summary.notSensitiveCount)
        assertEquals(0, summary.recommendationsCount)
    }

    @Test
    fun `getSummary aggregates multiple columns across tables`() {
        val col1 = makeSensitivity("users", "email")      // AT_RISK
        val col2 = makeSensitivity("users", "ssn", type = SensitivityType.SSN)  // AT_RISK
        val col3 = makeSensitivity("orders", "card", type = SensitivityType.CREDIT_CARD)  // AT_RISK
        val col4 = makeSensitivity("orders", "total", isSensitive = false, type = SensitivityType.UNKNOWN, confidence = ConfidenceLevel.LOW, recommendedType = null)  // NOT_SENSITIVE

        val tc = makeTableConfig(10L, "users")
        val emailGen = makeGenerator(10L, "email")

        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(col1, col2, col3, col4))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(listOf(tc))
        whenever(columnGeneratorRepository.findByTableConfigurationId(10L)).thenReturn(listOf(emailGen))

        val summary = service.getSummary(1L)

        // email is protected, ssn and card are at-risk, total is not sensitive
        assertEquals(2, summary.atRiskCount)
        assertEquals(1, summary.protectedCount)
        assertEquals(1, summary.notSensitiveCount)
        assertEquals(2, summary.tables.size)
    }

    // ── getRecommendations tests ──────────────────────────────────────────

    @Test
    fun `getRecommendations returns only sensitive and unprotected columns`() {
        val atRisk = makeSensitivity("users", "email")
        val protected = makeSensitivity("users", "phone", type = SensitivityType.PHONE, recommendedType = GeneratorType.PHONE)
        val notSensitive = makeSensitivity("orders", "total", isSensitive = false, type = SensitivityType.UNKNOWN, confidence = ConfidenceLevel.LOW, recommendedType = null)

        val tc = makeTableConfig(10L, "users")
        val phoneGen = makeGenerator(10L, "phone", GeneratorType.PHONE)

        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(atRisk, protected, notSensitive))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(listOf(tc))
        whenever(columnGeneratorRepository.findByTableConfigurationId(10L)).thenReturn(listOf(phoneGen))

        val recommendations = service.getRecommendations(1L)

        assertEquals(1, recommendations.size)
        assertEquals("email", recommendations[0].columnName)
        assertEquals("EMAIL", recommendations[0].sensitivityType)
        assertEquals("EMAIL", recommendations[0].recommendedGenerator)
    }

    @Test
    fun `getRecommendations returns empty list when no at-risk columns`() {
        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(emptyList())
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(emptyList())

        val recommendations = service.getRecommendations(1L)

        assertTrue(recommendations.isEmpty())
    }

    // ── applyRecommendations tests ────────────────────────────────────────

    @Test
    fun `applyRecommendations creates ColumnGenerators for at-risk columns`() {
        val atRisk = makeSensitivity("users", "email")
        val tc = makeTableConfig(10L, "users")

        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(atRisk))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(listOf(tc))
        whenever(columnGeneratorRepository.findByTableConfigurationId(10L)).thenReturn(emptyList())
        whenever(columnGeneratorRepository.save(any<ColumnGenerator>()))
            .thenAnswer { it.arguments[0] as ColumnGenerator }

        val count = service.applyRecommendations(1L)

        assertEquals(1, count)
        val captor = argumentCaptor<ColumnGenerator>()
        verify(columnGeneratorRepository).save(captor.capture())
        assertEquals(10L, captor.firstValue.tableConfigurationId)
        assertEquals("email", captor.firstValue.columnName)
        assertEquals(GeneratorType.EMAIL, captor.firstValue.generatorType)
    }

    @Test
    fun `applyRecommendations skips columns without matching table configuration`() {
        val atRisk = makeSensitivity("unknown_table", "email")

        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(atRisk))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(emptyList())

        val count = service.applyRecommendations(1L)

        assertEquals(0, count)
        verify(columnGeneratorRepository, never()).save(any())
    }

    @Test
    fun `applyRecommendations skips columns with blank recommended generator`() {
        val col = makeSensitivity("users", "misc", recommendedType = null)
        val tc = makeTableConfig(10L, "users")

        whenever(columnSensitivityRepository.findByWorkspaceId(1L)).thenReturn(listOf(col))
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(listOf(tc))
        whenever(columnGeneratorRepository.findByTableConfigurationId(10L)).thenReturn(emptyList())

        val count = service.applyRecommendations(1L)

        assertEquals(0, count)
        verify(columnGeneratorRepository, never()).save(any())
    }
}

