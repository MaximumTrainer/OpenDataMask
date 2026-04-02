package com.opendatamask.service

import com.opendatamask.config.EncryptionService
import com.opendatamask.adapter.output.connector.ConnectorFactory
import com.opendatamask.adapter.output.connector.DatabaseConnector
import com.opendatamask.domain.model.*
import com.opendatamask.adapter.output.persistence.ColumnGeneratorRepository
import com.opendatamask.adapter.output.persistence.DataConnectionRepository
import com.opendatamask.adapter.output.persistence.TableConfigurationRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class DataPreviewServiceTest {

    @Mock private lateinit var connectorFactory: ConnectorFactory
    @Mock private lateinit var dataConnectionRepository: DataConnectionRepository
    @Mock private lateinit var tableConfigurationRepository: TableConfigurationRepository
    @Mock private lateinit var columnGeneratorRepository: ColumnGeneratorRepository
    @Mock private lateinit var encryptionService: EncryptionService

    private lateinit var generatorService: GeneratorService
    private lateinit var dataPreviewService: DataPreviewService

    @BeforeEach
    fun setup() {
        generatorService = GeneratorService("0123456789abcdef")
        dataPreviewService = DataPreviewService(
            connectorFactory,
            dataConnectionRepository,
            tableConfigurationRepository,
            columnGeneratorRepository,
            generatorService,
            encryptionService
        )
    }

    private fun makeConnection(workspaceId: Long = 1L, isSource: Boolean = true) = DataConnection(
        id = 1L,
        workspaceId = workspaceId,
        name = "Source DB",
        type = ConnectionType.POSTGRESQL,
        connectionString = "encrypted_conn",
        isSource = isSource
    )

    @Test
    fun `previewColumn returns empty samples when no source connection`() {
        whenever(dataConnectionRepository.findByWorkspaceIdAndIsSource(1L, true)).thenReturn(emptyList())

        val result = dataPreviewService.previewColumn(1L, "users", "email")

        assertEquals("users", result.tableName)
        assertEquals("email", result.columnName)
        assertNull(result.generatorType)
        assertTrue(result.samples.isEmpty())
    }

    @Test
    fun `previewColumn returns original values when no generator configured`() {
        val mockConnector = mock<DatabaseConnector>()
        val conn = makeConnection()
        val rows = listOf(
            mapOf<String, Any?>("email" to "a@test.com"),
            mapOf<String, Any?>("email" to "b@test.com")
        )

        whenever(dataConnectionRepository.findByWorkspaceIdAndIsSource(1L, true)).thenReturn(listOf(conn))
        whenever(encryptionService.decrypt("encrypted_conn")).thenReturn("jdbc:postgresql://localhost/db")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(mockConnector)
        whenever(mockConnector.fetchData("users", limit = 5)).thenReturn(rows)
        whenever(tableConfigurationRepository.findByWorkspaceIdAndTableName(1L, "users")).thenReturn(Optional.empty())

        val result = dataPreviewService.previewColumn(1L, "users", "email")

        assertEquals(2, result.samples.size)
        assertNull(result.generatorType)
        assertEquals("a@test.com", result.samples[0].originalValue)
        assertEquals("a@test.com", result.samples[0].maskedValue, "Without generator, masked = original")
    }

    @Test
    fun `previewColumn masks values using configured generator`() {
        val mockConnector = mock<DatabaseConnector>()
        val conn = makeConnection()
        val rows = listOf(
            mapOf<String, Any?>("email" to "alice@example.com"),
            mapOf<String, Any?>("email" to "bob@example.com"),
            mapOf<String, Any?>("email" to "charlie@example.com")
        )
        val tableConfig = TableConfiguration(id = 99L, workspaceId = 1L, tableName = "users", mode = TableMode.MASK)
        val generator = ColumnGenerator(
            tableConfigurationId = 99L,
            columnName = "email",
            generatorType = GeneratorType.EMAIL
        )

        whenever(dataConnectionRepository.findByWorkspaceIdAndIsSource(1L, true)).thenReturn(listOf(conn))
        whenever(encryptionService.decrypt("encrypted_conn")).thenReturn("jdbc:postgresql://localhost/db")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(mockConnector)
        whenever(mockConnector.fetchData("users", limit = 5)).thenReturn(rows)
        whenever(tableConfigurationRepository.findByWorkspaceIdAndTableName(1L, "users")).thenReturn(Optional.of(tableConfig))
        whenever(columnGeneratorRepository.findByTableConfigurationId(99L)).thenReturn(listOf(generator))

        val result = dataPreviewService.previewColumn(1L, "users", "email")

        assertEquals(GeneratorType.EMAIL, result.generatorType)
        assertEquals(3, result.samples.size)
        result.samples.forEach { sample ->
            assertNotNull(sample.originalValue)
            assertNotNull(sample.maskedValue)
            assertTrue(sample.maskedValue!!.contains("@"), "Masked email should contain @")
        }
    }

    @Test
    fun `previewColumn respects sampleSize`() {
        val mockConnector = mock<DatabaseConnector>()
        val conn = makeConnection()
        val rows = (1..10).map { mapOf<String, Any?>("id" to it.toString()) }

        whenever(dataConnectionRepository.findByWorkspaceIdAndIsSource(1L, true)).thenReturn(listOf(conn))
        whenever(encryptionService.decrypt("encrypted_conn")).thenReturn("jdbc:postgresql://localhost/db")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(mockConnector)
        whenever(mockConnector.fetchData("orders", limit = 3)).thenReturn(rows.take(3))
        whenever(tableConfigurationRepository.findByWorkspaceIdAndTableName(1L, "orders")).thenReturn(Optional.empty())

        val result = dataPreviewService.previewColumn(1L, "orders", "id", sampleSize = 3)

        assertEquals(3, result.samples.size)
    }

    @Test
    fun `previewColumn returns empty samples when connector throws`() {
        val conn = makeConnection()

        whenever(dataConnectionRepository.findByWorkspaceIdAndIsSource(1L, true)).thenReturn(listOf(conn))
        whenever(encryptionService.decrypt("encrypted_conn")).thenReturn("jdbc:postgresql://localhost/db")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenThrow(RuntimeException("Connection refused"))

        val result = dataPreviewService.previewColumn(1L, "users", "email")

        assertTrue(result.samples.isEmpty(), "Should return empty samples on connector error")
    }

    @Test
    fun `previewColumn applies PARTIAL_MASK generator correctly`() {
        val mockConnector = mock<DatabaseConnector>()
        val conn = makeConnection()
        val rows = listOf(mapOf<String, Any?>("card" to "4111-1111-1111-1234"))
        val tableConfig = TableConfiguration(id = 50L, workspaceId = 1L, tableName = "payments", mode = TableMode.MASK)
        val generator = ColumnGenerator(
            tableConfigurationId = 50L,
            columnName = "card",
            generatorType = GeneratorType.PARTIAL_MASK,
            generatorParams = """{"maskEnd":"-4","maskChar":"*"}"""
        )

        whenever(dataConnectionRepository.findByWorkspaceIdAndIsSource(1L, true)).thenReturn(listOf(conn))
        whenever(encryptionService.decrypt("encrypted_conn")).thenReturn("jdbc:postgresql://localhost/db")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(mockConnector)
        whenever(mockConnector.fetchData("payments", limit = 5)).thenReturn(rows)
        whenever(tableConfigurationRepository.findByWorkspaceIdAndTableName(1L, "payments")).thenReturn(Optional.of(tableConfig))
        whenever(columnGeneratorRepository.findByTableConfigurationId(50L)).thenReturn(listOf(generator))

        val result = dataPreviewService.previewColumn(1L, "payments", "card")

        assertEquals(1, result.samples.size)
        assertEquals("4111-1111-1111-1234", result.samples[0].originalValue)
        assertEquals("****-****-****-1234", result.samples[0].maskedValue)
    }
}
