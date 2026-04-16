package com.opendatamask.application.service

import com.opendatamask.domain.port.output.EncryptionPort
import com.opendatamask.adapter.output.connector.ColumnInfo
import com.opendatamask.adapter.output.connector.ConnectorFactory
import com.opendatamask.adapter.output.connector.DatabaseConnector
import com.opendatamask.domain.model.*
import com.opendatamask.adapter.output.persistence.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SensitivityScanLogServiceTest {

    @Mock private lateinit var columnSensitivityRepository: ColumnSensitivityRepository
    @Mock private lateinit var sensitivityScanLogRepository: SensitivityScanLogRepository
    @Mock private lateinit var sensitivityScanLogEntryRepository: SensitivityScanLogEntryRepository
    @Mock private lateinit var workspaceRepository: WorkspaceRepository
    @Mock private lateinit var dataConnectionRepository: DataConnectionRepository
    @Mock private lateinit var connectorFactory: ConnectorFactory
    @Mock private lateinit var EncryptionPort: EncryptionPort
    @Mock private lateinit var customSensitivityRuleRepository: CustomSensitivityRuleRepository
    @Mock private lateinit var customSensitivityRuleService: CustomSensitivityRuleService

    @InjectMocks
    private lateinit var sensitivityScanService: SensitivityScanService

    @Test
    fun `scanWorkspace writes a log entry for each column scanned`() {
        val workspace = Workspace(id = 1L, name = "WS", ownerId = 1L)
        val connection = DataConnection(
            id = 1L, workspaceId = 1L, name = "src",
            type = ConnectionType.POSTGRESQL,
            connectionString = "enc",
            isSource = true
        )
        val scanLog = SensitivityScanLog(id = 5L, workspaceId = 1L)
        val mockConnector = mock<DatabaseConnector>()

        val columns = listOf(
            ColumnInfo("email", "varchar"),
            ColumnInfo("order_id", "bigint")
        )
        val sampleRows = listOf(mapOf<String, Any?>("email" to "a@b.com", "order_id" to 1L))

        whenever(sensitivityScanLogRepository.save(any<SensitivityScanLog>())).thenReturn(scanLog)
        whenever(sensitivityScanLogEntryRepository.save(any<SensitivityScanLogEntry>()))
            .thenAnswer { it.arguments[0] as SensitivityScanLogEntry }
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace))
        whenever(dataConnectionRepository.findByWorkspaceIdAndIsSource(1L, true))
            .thenReturn(listOf(connection))
        whenever(EncryptionPort.decrypt("enc")).thenReturn("conn")
        whenever(connectorFactory.createConnector(
            eq(ConnectionType.POSTGRESQL), eq("conn"),
            anyOrNull(), anyOrNull(), anyOrNull()
        )).thenReturn(mockConnector)
        whenever(mockConnector.listTables()).thenReturn(listOf("users"))
        whenever(mockConnector.listColumns("users")).thenReturn(columns)
        whenever(mockConnector.fetchData(eq("users"), eq(100), anyOrNull(), anyOrNull())).thenReturn(sampleRows)
        whenever(columnSensitivityRepository.findByWorkspaceIdAndTableNameAndColumnName(any(), any(), any()))
            .thenReturn(null)
        whenever(columnSensitivityRepository.save(any<ColumnSensitivity>()))
            .thenAnswer { it.arguments[0] as ColumnSensitivity }
        whenever(customSensitivityRuleRepository.findByIsActiveTrue()).thenReturn(emptyList())

        sensitivityScanService.scanWorkspace(1L)

        // One entry per column (2 columns)
        val entryCaptor = argumentCaptor<SensitivityScanLogEntry>()
        verify(sensitivityScanLogEntryRepository, times(2)).save(entryCaptor.capture())

        val entries = entryCaptor.allValues
        val emailEntry = entries.first { it.columnName == "email" }
        val orderEntry = entries.first { it.columnName == "order_id" }

        assertEquals(5L, emailEntry.scanLogId)
        assertEquals("users", emailEntry.tableName)
        assertEquals("EMAIL", emailEntry.detectedType)
        assertNotNull(emailEntry.confidenceLevel)

        assertEquals(5L, orderEntry.scanLogId)
        assertEquals("users", orderEntry.tableName)
        assertNull(orderEntry.detectedType)
        assertNull(orderEntry.confidenceLevel)
    }

    @Test
    fun `scanWorkspace entries contain recommended generator for sensitive columns`() {
        val workspace = Workspace(id = 1L, name = "WS", ownerId = 1L)
        val connection = DataConnection(
            id = 1L, workspaceId = 1L, name = "src",
            type = ConnectionType.POSTGRESQL,
            connectionString = "enc",
            isSource = true
        )
        val scanLog = SensitivityScanLog(id = 7L, workspaceId = 1L)
        val mockConnector = mock<DatabaseConnector>()

        val columns = listOf(ColumnInfo("ssn", "varchar"))
        val sampleRows = listOf(mapOf<String, Any?>("ssn" to "123-45-6789"))

        whenever(sensitivityScanLogRepository.save(any<SensitivityScanLog>())).thenReturn(scanLog)
        whenever(sensitivityScanLogEntryRepository.save(any<SensitivityScanLogEntry>()))
            .thenAnswer { it.arguments[0] as SensitivityScanLogEntry }
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace))
        whenever(dataConnectionRepository.findByWorkspaceIdAndIsSource(1L, true))
            .thenReturn(listOf(connection))
        whenever(EncryptionPort.decrypt("enc")).thenReturn("conn")
        whenever(connectorFactory.createConnector(
            eq(ConnectionType.POSTGRESQL), eq("conn"),
            anyOrNull(), anyOrNull(), anyOrNull()
        )).thenReturn(mockConnector)
        whenever(mockConnector.listTables()).thenReturn(listOf("users"))
        whenever(mockConnector.listColumns("users")).thenReturn(columns)
        whenever(mockConnector.fetchData(eq("users"), eq(100), anyOrNull(), anyOrNull())).thenReturn(sampleRows)
        whenever(columnSensitivityRepository.findByWorkspaceIdAndTableNameAndColumnName(any(), any(), any()))
            .thenReturn(null)
        whenever(columnSensitivityRepository.save(any<ColumnSensitivity>()))
            .thenAnswer { it.arguments[0] as ColumnSensitivity }
        whenever(customSensitivityRuleRepository.findByIsActiveTrue()).thenReturn(emptyList())

        sensitivityScanService.scanWorkspace(1L)

        val entryCaptor = argumentCaptor<SensitivityScanLogEntry>()
        verify(sensitivityScanLogEntryRepository).save(entryCaptor.capture())
        val entry = entryCaptor.firstValue
        assertEquals("ssn", entry.columnName)
        assertEquals("SSN", entry.detectedType)
        assertNotNull(entry.recommendedGenerator)
    }
}

