package com.opendatamask.service

import com.opendatamask.config.EncryptionService
import com.opendatamask.connector.ConnectorFactory
import com.opendatamask.connector.DatabaseConnector
import com.opendatamask.dto.DataConnectionRequest
import com.opendatamask.domain.model.ConnectionType
import com.opendatamask.domain.model.DataConnection
import com.opendatamask.adapter.output.persistence.DataConnectionRepository
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

    @Mock private lateinit var dataConnectionRepository: DataConnectionRepository
    @Mock private lateinit var encryptionService: EncryptionService
    @Mock private lateinit var connectorFactory: ConnectorFactory

    @InjectMocks
    private lateinit var service: DataConnectionService

    private fun makeRequest(
        name: String = "My DB",
        type: ConnectionType = ConnectionType.POSTGRESQL,
        connectionString: String = "jdbc:postgresql://localhost/db",
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
        whenever(encryptionService.encrypt("jdbc:postgresql://localhost/db")).thenReturn("encrypted_conn")
        whenever(encryptionService.encrypt("pass")).thenReturn("encrypted_pass")
        whenever(dataConnectionRepository.save(any<DataConnection>())).thenReturn(saved)

        val response = service.createConnection(10L, request)

        assertEquals(1L, response.id)
        assertEquals(10L, response.workspaceId)
        verify(encryptionService).encrypt("jdbc:postgresql://localhost/db")
        verify(encryptionService).encrypt("pass")
    }

    @Test
    fun `createConnection handles null password`() {
        val request = makeRequest(password = null)
        val saved = makeConnection(id = 1L).also { it.password = null }
        whenever(encryptionService.encrypt(any())).thenReturn("encrypted_conn")
        whenever(dataConnectionRepository.save(any<DataConnection>())).thenReturn(saved)

        service.createConnection(10L, request)

        verify(encryptionService, times(1)).encrypt(any())  // Only conn string encrypted
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

    // ── updateConnection ───────────────────────────────────────────────────

    @Test
    fun `updateConnection updates fields and saves`() {
        val conn = makeConnection(id = 1L, workspaceId = 10L)
        val updateRequest = makeRequest(name = "Updated DB", connectionString = "jdbc:updated")
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(conn))
        whenever(encryptionService.encrypt("jdbc:updated")).thenReturn("enc_updated")
        whenever(encryptionService.encrypt("pass")).thenReturn("enc_pass")
        whenever(dataConnectionRepository.save(any<DataConnection>())).thenAnswer { it.arguments[0] as DataConnection }

        val response = service.updateConnection(10L, 1L, updateRequest)

        assertEquals("Updated DB", response.name)
        verify(dataConnectionRepository).save(any())
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

        verify(dataConnectionRepository).delete(conn)
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
        whenever(encryptionService.decrypt("encrypted_conn")).thenReturn("real_conn")
        whenever(encryptionService.decrypt("encrypted_pass")).thenReturn("real_pass")
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
        whenever(encryptionService.decrypt(any())).thenReturn("decrypted")
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
        whenever(encryptionService.decrypt(any())).thenReturn("decrypted")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenThrow(RuntimeException("host unreachable"))

        val result = service.testConnection(10L, 1L)

        assertFalse(result.success)
        assertTrue(result.message.contains("host unreachable"))
    }
}
