package com.opendatamask.service

import com.opendatamask.config.EncryptionService
import com.opendatamask.connector.ColumnInfo
import com.opendatamask.connector.ConnectorFactory
import com.opendatamask.connector.DatabaseConnector
import com.opendatamask.domain.model.*
import com.opendatamask.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SensitivityScanServiceTest {

    @Mock private lateinit var columnSensitivityRepository: ColumnSensitivityRepository
    @Mock private lateinit var sensitivityScanLogRepository: SensitivityScanLogRepository
    @Mock private lateinit var sensitivityScanLogEntryRepository: SensitivityScanLogEntryRepository
    @Mock private lateinit var workspaceRepository: WorkspaceRepository
    @Mock private lateinit var dataConnectionRepository: DataConnectionRepository
    @Mock private lateinit var connectorFactory: ConnectorFactory
    @Mock private lateinit var encryptionService: EncryptionService

    @InjectMocks
    private lateinit var sensitivityScanService: SensitivityScanService

    private fun makeWorkspace(id: Long = 1L) = Workspace(id = id, name = "WS", ownerId = 1L)

    private fun makeConnection(id: Long = 1L) = DataConnection(
        id = id, workspaceId = 1L, name = "src",
        type = ConnectionType.POSTGRESQL,
        connectionString = "enc_conn_string",
        isSource = true
    )

    private fun makeScanLog(id: Long = 1L, workspaceId: Long = 1L) =
        SensitivityScanLog(id = id, workspaceId = workspaceId)

    // ── detectSensitivity unit tests ───────────────────────────────────────

    @Test
    fun `detectSensitivity returns EMAIL with FULL confidence when column name and value both match`() {
        val result = sensitivityScanService.detectSensitivity("email", listOf("john@example.com"))
        assertNotNull(result)
        assertEquals(SensitivityType.EMAIL, result!!.sensitivityType)
        assertEquals(ConfidenceLevel.FULL, result.confidence)
    }

    @Test
    fun `detectSensitivity returns EMAIL with HIGH confidence when only column name matches`() {
        val result = sensitivityScanService.detectSensitivity("email_address", emptyList())
        assertNotNull(result)
        assertEquals(SensitivityType.EMAIL, result!!.sensitivityType)
        assertEquals(ConfidenceLevel.HIGH, result.confidence)
    }

    @Test
    fun `detectSensitivity returns SSN for ssn column`() {
        val result = sensitivityScanService.detectSensitivity("ssn", listOf("123-45-6789"))
        assertNotNull(result)
        assertEquals(SensitivityType.SSN, result!!.sensitivityType)
        assertEquals(ConfidenceLevel.FULL, result.confidence)
    }

    @Test
    fun `detectSensitivity returns FIRST_NAME for first_name column`() {
        val result = sensitivityScanService.detectSensitivity("first_name", emptyList())
        assertNotNull(result)
        assertEquals(SensitivityType.FIRST_NAME, result!!.sensitivityType)
        assertEquals(ConfidenceLevel.HIGH, result.confidence)
    }

    @Test
    fun `detectSensitivity returns CITY for city column`() {
        val result = sensitivityScanService.detectSensitivity("city", emptyList())
        assertNotNull(result)
        assertEquals(SensitivityType.CITY, result!!.sensitivityType)
    }

    @Test
    fun `detectSensitivity returns CREDIT_CARD with FULL when both column and value match`() {
        val result = sensitivityScanService.detectSensitivity(
            "credit_card_number",
            listOf("4111-1111-1111-1111")
        )
        assertNotNull(result)
        assertEquals(SensitivityType.CREDIT_CARD, result!!.sensitivityType)
        assertEquals(ConfidenceLevel.FULL, result.confidence)
    }

    @Test
    fun `detectSensitivity returns CREDIT_CARD with HIGH when only column name matches`() {
        val result = sensitivityScanService.detectSensitivity("credit_card_number", emptyList())
        assertNotNull(result)
        assertEquals(SensitivityType.CREDIT_CARD, result!!.sensitivityType)
        assertEquals(ConfidenceLevel.HIGH, result.confidence)
    }

    @Test
    fun `detectSensitivity returns PASSWORD with MEDIUM confidence`() {
        val result = sensitivityScanService.detectSensitivity("password_hash", emptyList())
        assertNotNull(result)
        assertEquals(SensitivityType.PASSWORD, result!!.sensitivityType)
        assertEquals(ConfidenceLevel.MEDIUM, result.confidence)
    }

    @Test
    fun `detectSensitivity returns null for non-sensitive column`() {
        val result = sensitivityScanService.detectSensitivity("order_total", listOf("99.99"))
        assertNull(result)
    }

    // ── scanWorkspace integration tests ────────────────────────────────────

    @Test
    fun `scanWorkspace completes with correct counts and column sensitivity results`() {
        val workspace = makeWorkspace()
        val connection = makeConnection()
        val mockConnector = mock<DatabaseConnector>()
        val scanLog = makeScanLog()

        val columns = listOf(
            ColumnInfo("email", "varchar"),
            ColumnInfo("ssn", "varchar"),
            ColumnInfo("phone_number", "varchar"),
            ColumnInfo("first_name", "varchar"),
            ColumnInfo("credit_card_number", "varchar"),
            ColumnInfo("city", "varchar")
        )

        val sampleRows = listOf(
            mapOf<String, Any?>(
                "email" to "john@example.com",
                "ssn" to "123-45-6789",
                "phone_number" to "555-123-4567",
                "first_name" to "John",
                "credit_card_number" to "4111-1111-1111-1111",
                "city" to "New York"
            )
        )

        whenever(sensitivityScanLogRepository.save(any<SensitivityScanLog>())).thenReturn(scanLog)
        whenever(sensitivityScanLogEntryRepository.save(any<SensitivityScanLogEntry>()))
            .thenAnswer { it.arguments[0] as SensitivityScanLogEntry }
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace))
        whenever(dataConnectionRepository.findByWorkspaceIdAndIsSource(1L, true))
            .thenReturn(listOf(connection))
        whenever(encryptionService.decrypt("enc_conn_string")).thenReturn("conn_string")
        whenever(connectorFactory.createConnector(
            eq(ConnectionType.POSTGRESQL), eq("conn_string"),
            anyOrNull(), anyOrNull(), anyOrNull()
        )).thenReturn(mockConnector)
        whenever(mockConnector.listTables()).thenReturn(listOf("users"))
        whenever(mockConnector.listColumns("users")).thenReturn(columns)
        whenever(mockConnector.fetchData(eq("users"), eq(100), anyOrNull())).thenReturn(sampleRows)
        whenever(columnSensitivityRepository.findByWorkspaceIdAndTableNameAndColumnName(
            any(), any(), any()
        )).thenReturn(null)
        whenever(columnSensitivityRepository.save(any<ColumnSensitivity>()))
            .thenAnswer { it.arguments[0] as ColumnSensitivity }

        val result = sensitivityScanService.scanWorkspace(1L)

        assertEquals("COMPLETED", result.status)
        assertEquals(1, result.tablesScanned)
        assertEquals(6, result.columnsScanned)
        assertEquals(6, result.sensitiveColumnsFound)

        // Verify specific column types saved
        val savedCaptor = argumentCaptor<ColumnSensitivity>()
        verify(columnSensitivityRepository, times(6)).save(savedCaptor.capture())

        val emailResult = savedCaptor.allValues.first { it.columnName == "email" }
        assertEquals(SensitivityType.EMAIL, emailResult.sensitivityType)
        assertEquals(ConfidenceLevel.FULL, emailResult.confidenceLevel)

        val ssnResult = savedCaptor.allValues.first { it.columnName == "ssn" }
        assertEquals(SensitivityType.SSN, ssnResult.sensitivityType)
        assertEquals(ConfidenceLevel.FULL, ssnResult.confidenceLevel)

        val cityResult = savedCaptor.allValues.first { it.columnName == "city" }
        assertEquals(SensitivityType.CITY, cityResult.sensitivityType)

        val firstNameResult = savedCaptor.allValues.first { it.columnName == "first_name" }
        assertEquals(SensitivityType.FIRST_NAME, firstNameResult.sensitivityType)
    }

    @Test
    fun `scanWorkspace fails with FAILED status when no source connection`() {
        val scanLog = makeScanLog()
        whenever(sensitivityScanLogRepository.save(any<SensitivityScanLog>())).thenReturn(scanLog)
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))
        whenever(dataConnectionRepository.findByWorkspaceIdAndIsSource(1L, true))
            .thenReturn(emptyList())

        val result = sensitivityScanService.scanWorkspace(1L)

        assertEquals("FAILED", result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `scanWorkspace fails with FAILED status when workspace not found`() {
        val scanLog = makeScanLog()
        whenever(sensitivityScanLogRepository.save(any<SensitivityScanLog>())).thenReturn(scanLog)
        whenever(workspaceRepository.findById(99L)).thenReturn(Optional.empty())

        val result = sensitivityScanService.scanWorkspace(99L)

        assertEquals("FAILED", result.status)
    }
}
