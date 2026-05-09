package com.opendatamask.application.service

import com.opendatamask.domain.model.SubsetLimitType
import com.opendatamask.domain.model.SubsetTableConfig
import com.opendatamask.domain.port.output.DataConnectionPort
import com.opendatamask.domain.port.output.EncryptionPort
import com.opendatamask.domain.port.output.SubsetTableConfigPort
import com.opendatamask.domain.port.output.ConnectorFactoryPort
import com.opendatamask.domain.model.DataConnection
import com.opendatamask.domain.model.ConnectionType
import com.opendatamask.domain.port.output.DatabaseConnector
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(MockitoExtension::class)
class SubsetEstimationServiceTest {

    @Mock private lateinit var subsetTableConfigPort: SubsetTableConfigPort
    @Mock private lateinit var dataConnectionPort: DataConnectionPort
    @Mock private lateinit var encryptionPort: EncryptionPort
    @Mock private lateinit var connectorFactory: ConnectorFactoryPort

    @InjectMocks private lateinit var service: SubsetEstimationService

    private fun makeTableConfig(
        tableName: String = "users",
        limitType: SubsetLimitType = SubsetLimitType.PERCENTAGE,
        limitValue: Int = 10,
        workspaceId: Long = 1L
    ) = SubsetTableConfig(
        id = 1L, workspaceId = workspaceId, tableName = tableName,
        limitType = limitType, limitValue = limitValue
    )

    private fun makeSource(workspaceId: Long = 1L) = DataConnection(
        id = 1L, workspaceId = workspaceId, name = "src",
        type = ConnectionType.POSTGRESQL, connectionString = "enc",
        isSource = true, isDestination = false
    )

    private fun stubConnector(workspaceId: Long = 1L, connector: DatabaseConnector) {
        val source = makeSource(workspaceId)
        whenever(dataConnectionPort.findByWorkspaceId(workspaceId)).thenReturn(listOf(source))
        whenever(encryptionPort.decrypt("enc")).thenReturn("jdbc:postgresql://localhost/db")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(connector)
    }

    @Test
    fun `should return estimated row count for PERCENTAGE limit`() {
        val connector = mock<DatabaseConnector>()
        stubConnector(connector = connector)
        whenever(subsetTableConfigPort.findByWorkspaceId(1L)).thenReturn(listOf(makeTableConfig(limitValue = 10)))
        whenever(connector.countRows("users", null)).thenReturn(1000L)

        val result = service.estimate(1L)

        assertEquals(1, result.tableEstimates.size)
        assertEquals("users", result.tableEstimates[0].tableName)
        assertEquals(1000L, result.tableEstimates[0].totalRows)
        assertEquals(100L, result.tableEstimates[0].estimatedRows) // 10% of 1000
        assertEquals(SubsetLimitType.PERCENTAGE, result.tableEstimates[0].limitType)
    }

    @Test
    fun `should return estimated row count for ROW_COUNT limit`() {
        val connector = mock<DatabaseConnector>()
        stubConnector(connector = connector)
        whenever(subsetTableConfigPort.findByWorkspaceId(1L)).thenReturn(
            listOf(makeTableConfig(limitType = SubsetLimitType.ROW_COUNT, limitValue = 50))
        )
        whenever(connector.countRows("users", null)).thenReturn(200L)

        val result = service.estimate(1L)

        assertEquals(50L, result.tableEstimates[0].estimatedRows)
        assertEquals(200L, result.tableEstimates[0].totalRows)
    }

    @Test
    fun `should cap ROW_COUNT estimate at total rows when limit exceeds source`() {
        val connector = mock<DatabaseConnector>()
        stubConnector(connector = connector)
        whenever(subsetTableConfigPort.findByWorkspaceId(1L)).thenReturn(
            listOf(makeTableConfig(limitType = SubsetLimitType.ROW_COUNT, limitValue = 5000))
        )
        whenever(connector.countRows("users", null)).thenReturn(200L)

        val result = service.estimate(1L)

        assertEquals(200L, result.tableEstimates[0].estimatedRows)
    }

    @Test
    fun `should return all rows for ALL limit type`() {
        val connector = mock<DatabaseConnector>()
        stubConnector(connector = connector)
        whenever(subsetTableConfigPort.findByWorkspaceId(1L)).thenReturn(
            listOf(makeTableConfig(limitType = SubsetLimitType.ALL, limitValue = 0))
        )
        whenever(connector.countRows("users", null)).thenReturn(500L)

        val result = service.estimate(1L)

        assertEquals(500L, result.tableEstimates[0].estimatedRows)
        assertEquals(500L, result.tableEstimates[0].totalRows)
    }

    @Test
    fun `should aggregate total estimated rows across all tables`() {
        val connector = mock<DatabaseConnector>()
        stubConnector(connector = connector)
        whenever(subsetTableConfigPort.findByWorkspaceId(1L)).thenReturn(
            listOf(
                makeTableConfig(tableName = "users", limitValue = 10),
                makeTableConfig(tableName = "orders", limitValue = 20)
            )
        )
        whenever(connector.countRows("users", null)).thenReturn(1000L)
        whenever(connector.countRows("orders", null)).thenReturn(500L)

        val result = service.estimate(1L)

        assertEquals(2, result.tableEstimates.size)
        // 10% of 1000 + 20% of 500 = 100 + 100 = 200
        assertEquals(200L, result.totalEstimatedRows)
    }

    @Test
    fun `should fail fast when no source connection configured`() {
        whenever(dataConnectionPort.findByWorkspaceId(1L)).thenReturn(emptyList())

        val result = service.estimate(1L)

        assertTrue(result.tableEstimates.isEmpty())
        assertFalse(result.success)
        assertTrue(result.errorMessage?.contains("source") == true, "Error should mention source connection")
    }

    @Test
    fun `should handle empty subset config gracefully`() {
        // stub only the connection lookup — connector creation won't be reached (configs are empty)
        whenever(dataConnectionPort.findByWorkspaceId(1L)).thenReturn(listOf(makeSource()))
        whenever(subsetTableConfigPort.findByWorkspaceId(1L)).thenReturn(emptyList())

        val result = service.estimate(1L)

        assertTrue(result.tableEstimates.isEmpty())
        assertEquals(0L, result.totalEstimatedRows)
        assertTrue(result.success)
    }
}
