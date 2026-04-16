package com.opendatamask.application.service

import com.opendatamask.domain.port.output.EncryptionPort
import com.opendatamask.domain.port.output.ConnectorFactoryPort
import com.opendatamask.domain.port.output.DatabaseConnector
import com.opendatamask.domain.port.output.DataConnectionPort
import com.opendatamask.domain.port.input.dto.DataConnectionRequest
import com.opendatamask.domain.model.ConnectionType
import com.opendatamask.domain.model.DataConnection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class DataConnectionServiceTest {

    @Mock private lateinit var dataConnectionRepository: DataConnectionPort
    @Mock private lateinit var EncryptionPort: EncryptionPort
    @Mock private lateinit var connectorFactory: ConnectorFactoryPort

    @InjectMocks
    private lateinit var service: DataConnectionService

    private fun makeRequest(
        name: String = "My DB",
        type: ConnectionType = ConnectionType.POSTGRESQL,
        connectionString: String? = "jdbc:postgresql://localhost/db",
        username: String? = "user",
        password: String? = "pass",
        database: String? = null,
        isSource: Boolean = true,
        isDestination: Boolean = false
    ) = DataConnectionRequest(
        name = name, type = type, connectionString = connectionString,
        username = username, password = password, database = database,
        isSource = isSource, isDestination = isDestination
    )

    private fun makeConnection(
        id: Long = 1L,
        workspaceId: Long = 10L,
        isSource: Boolean = true,
        isDestination: Boolean = false
    ) = DataConnection(
        id = id, workspaceId = workspaceId, name = "My DB",
        type = ConnectionType.POSTGRESQL,
        connectionString = "encrypted_conn",
        username = "user", password = "encrypted_pass",
        isSource = isSource, isDestination = isDestination,
        createdAt = LocalDateTime.now()
    )

    // ── createConnection ───────────────────────────────────────────────────

    @Test
    fun `createConnection encrypts credentials and saves`() {
        val request = makeRequest()
        val saved = makeConnection(id = 1L)
        whenever(EncryptionPort.encrypt("jdbc:postgresql://localhost/db")).thenReturn("encrypted_conn")
        whenever(EncryptionPort.encrypt("pass")).thenReturn("encrypted_pass")
        whenever(dataConnectionRepository.save(any<DataConnection>())).thenReturn(saved)

        val response = service.createConnection(10L, request)

        assertEquals(1L, response.id)
        assertEquals(10L, response.workspaceId)
        verify(EncryptionPort).encrypt("jdbc:postgresql://localhost/db")
        verify(EncryptionPort).encrypt("pass")
    }

    @Test
    fun `createConnection handles null password`() {
        val request = makeRequest(password = null)
        val saved = makeConnection(id = 1L).also { it.password = null }
        whenever(EncryptionPort.encrypt(any())).thenReturn("encrypted_conn")
        whenever(dataConnectionRepository.save(any<DataConnection>())).thenReturn(saved)

        service.createConnection(10L, request)

        verify(EncryptionPort, times(1)).encrypt(any())  // Only conn string encrypted
    }

    @Test
    fun `createConnection throws for null connection string`() {
        val request = makeRequest(connectionString = null)
        assertThrows<IllegalArgumentException> { service.createConnection(10L, request) }
    }

    @Test
    fun `createConnection throws for blank connection string`() {
        val request = makeRequest(connectionString = "   ")
        assertThrows<IllegalArgumentException> { service.createConnection(10L, request) }
    }

    // ── getConnection ──────────────────────────────────────────────────────

    @Test
    fun `getConnection returns connection response`() {
        val conn = makeConnection(id = 1L, workspaceId = 10L)
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(conn))

        val response = service.getConnection(10L, 1L)

        assertEquals(1L, response.id)
        assertEquals(10L, response.workspaceId)
        assertEquals("My DB", response.name)
        assertEquals(ConnectionType.POSTGRESQL, response.type)
    }

    @Test
    fun `getConnection throws when connection not found`() {
        whenever(dataConnectionRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.getConnection(10L, 99L) }
    }

    @Test
    fun `getConnection throws when connection belongs to different workspace`() {
        val conn = makeConnection(id = 1L, workspaceId = 20L)
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(conn))

        assertThrows<NoSuchElementException> { service.getConnection(10L, 1L) }
    }

    // ── listConnections ────────────────────────────────────────────────────

    @Test
    fun `listConnections returns all connections for workspace`() {
        val conns = listOf(makeConnection(id = 1L), makeConnection(id = 2L))
        whenever(dataConnectionRepository.findByWorkspaceId(10L)).thenReturn(conns)

        val result = service.listConnections(10L)

        assertEquals(2, result.size)
    }

    @Test
    fun `listConnections returns empty list when none found`() {
        whenever(dataConnectionRepository.findByWorkspaceId(10L)).thenReturn(emptyList())

        val result = service.listConnections(10L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `createConnection extracts host from JDBC URL`() {
        val request = makeRequest(connectionString = "jdbc:postgresql://myhost:5432/mydb")
        whenever(EncryptionPort.encrypt(any())).thenReturn("encrypted")
        val captor = argumentCaptor<DataConnection>()
        whenever(dataConnectionRepository.save(captor.capture())).thenAnswer { it.arguments[0] as DataConnection }

        service.createConnection(10L, request)

        assertEquals("myhost:5432", captor.firstValue.host)
    }

    @Test
    fun `createConnection extracts host from Azure SQL JDBC URL ignoring semicolon params`() {
        val request = makeRequest(
            type = ConnectionType.AZURE_SQL,
            connectionString = "jdbc:sqlserver://myserver:1433;databaseName=mydb;encrypt=true"
        )
        whenever(EncryptionPort.encrypt(any())).thenReturn("encrypted")
        val captor = argumentCaptor<DataConnection>()
        whenever(dataConnectionRepository.save(captor.capture())).thenAnswer { it.arguments[0] as DataConnection }

        service.createConnection(10L, request)

        assertEquals("myserver:1433", captor.firstValue.host)
    }

    @Test
    fun `createConnection strips credentials from MongoDB URI when extracting host`() {
        val request = makeRequest(
            type = ConnectionType.MONGODB,
            connectionString = "mongodb://user:pass@mycluster:27017/mydb"
        )
        whenever(EncryptionPort.encrypt(any())).thenReturn("encrypted")
        val captor = argumentCaptor<DataConnection>()
        whenever(dataConnectionRepository.save(captor.capture())).thenAnswer { it.arguments[0] as DataConnection }

        service.createConnection(10L, request)

        assertEquals("mycluster:27017", captor.firstValue.host)
    }

    // ── updateConnection ───────────────────────────────────────────────────

    @Test
    fun `updateConnection updates fields and saves`() {
        val conn = makeConnection(id = 1L, workspaceId = 10L)
        val updateRequest = makeRequest(name = "Updated DB", connectionString = "jdbc:updated")
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(conn))
        whenever(EncryptionPort.encrypt("jdbc:updated")).thenReturn("enc_updated")
        whenever(EncryptionPort.encrypt("pass")).thenReturn("enc_pass")
        whenever(dataConnectionRepository.save(any<DataConnection>())).thenAnswer { it.arguments[0] as DataConnection }

        val response = service.updateConnection(10L, 1L, updateRequest)

        assertEquals("Updated DB", response.name)
        verify(dataConnectionRepository).save(any())
    }

    @Test
    fun `updateConnection keeps existing connection string when connectionString is null`() {
        val conn = makeConnection(id = 1L, workspaceId = 10L)
        val updateRequest = makeRequest(name = "Updated DB", connectionString = null)
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(conn))
        whenever(dataConnectionRepository.save(any<DataConnection>())).thenAnswer { it.arguments[0] as DataConnection }

        service.updateConnection(10L, 1L, updateRequest)

        // encrypt should NOT be called for connection string (no new string provided)
        verify(EncryptionPort, never()).encrypt(eq("jdbc:postgresql://localhost/db"))
    }

    @Test
    fun `updateConnection throws when type changes without new connection string`() {
        val conn = makeConnection(id = 1L, workspaceId = 10L) // type=POSTGRESQL
        // Attempt to switch to MONGODB without providing a new connection string
        val updateRequest = makeRequest(
            name = "Updated DB",
            type = ConnectionType.MONGODB,
            connectionString = null
        )
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(conn))

        assertThrows<IllegalArgumentException> {
            service.updateConnection(10L, 1L, updateRequest)
        }
    }

    @Test
    fun `updateConnection throws when connection not found`() {
        whenever(dataConnectionRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            service.updateConnection(10L, 99L, makeRequest())
        }
    }

    // ── deleteConnection ───────────────────────────────────────────────────

    @Test
    fun `deleteConnection removes connection`() {
        val conn = makeConnection(id = 1L, workspaceId = 10L)
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(conn))

        service.deleteConnection(10L, 1L)

        verify(dataConnectionRepository).deleteById(1L)
    }

    @Test
    fun `deleteConnection throws when connection not found`() {
        whenever(dataConnectionRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.deleteConnection(10L, 99L) }
    }

    // ── testConnection ─────────────────────────────────────────────────────

    @Test
    fun `testConnection returns success when connector connects`() {
        val conn = makeConnection(id = 1L, workspaceId = 10L)
        val mockConnector = mock<DatabaseConnector>()
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(conn))
        whenever(EncryptionPort.decrypt("encrypted_conn")).thenReturn("real_conn")
        whenever(EncryptionPort.decrypt("encrypted_pass")).thenReturn("real_pass")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(mockConnector)
        whenever(mockConnector.testConnection()).thenReturn(true)

        val result = service.testConnection(10L, 1L)

        assertTrue(result.success)
        assertEquals("Connection successful", result.message)
    }

    @Test
    fun `testConnection returns failure when connector fails`() {
        val conn = makeConnection(id = 1L, workspaceId = 10L)
        val mockConnector = mock<DatabaseConnector>()
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(conn))
        whenever(EncryptionPort.decrypt(any())).thenReturn("decrypted")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(mockConnector)
        whenever(mockConnector.testConnection()).thenReturn(false)

        val result = service.testConnection(10L, 1L)

        assertFalse(result.success)
        assertEquals("Connection failed", result.message)
    }

    @Test
    fun `testConnection returns failure message when connector throws`() {
        val conn = makeConnection(id = 1L, workspaceId = 10L)
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(conn))
        whenever(EncryptionPort.decrypt(any())).thenReturn("decrypted")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenThrow(RuntimeException("host unreachable"))

        val result = service.testConnection(10L, 1L)

        assertFalse(result.success)
        assertTrue(result.message.contains("host unreachable"))
    }

    // ── browseConnectionSchema ─────────────────────────────────────────────

    @Test
    fun `browseConnectionSchema returns tables and columns from connector`() {
        val conn = makeConnection(id = 1L, workspaceId = 10L)
        val mockConnector = mock<DatabaseConnector>()
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(conn))
        whenever(EncryptionPort.decrypt("encrypted_conn")).thenReturn("real_conn")
        whenever(EncryptionPort.decrypt("encrypted_pass")).thenReturn("real_pass")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(mockConnector)
        whenever(mockConnector.listTables()).thenReturn(listOf("users", "orders"))
        whenever(mockConnector.listColumns("users")).thenReturn(listOf(
            com.opendatamask.domain.port.output.ColumnInfo("id", "bigint", false),
            com.opendatamask.domain.port.output.ColumnInfo("email", "varchar", true)
        ))
        whenever(mockConnector.listColumns("orders")).thenReturn(listOf(
            com.opendatamask.domain.port.output.ColumnInfo("id", "bigint", false)
        ))

        val result = service.browseConnectionSchema(10L, 1L)

        assertEquals(1L, result.connectionId)
        assertEquals(2, result.tables.size)
        val usersTable = result.tables.find { it.tableName == "users" }!!
        assertEquals(2, usersTable.columns.size)
        assertEquals("email", usersTable.columns[1].name)
        assertTrue(usersTable.columns[1].nullable)
    }

    @Test
    fun `browseConnectionSchema throws when connection not found`() {
        whenever(dataConnectionRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.browseConnectionSchema(10L, 99L) }
    }
}