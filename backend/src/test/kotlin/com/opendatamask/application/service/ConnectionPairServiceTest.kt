package com.opendatamask.application.service

import com.opendatamask.domain.port.output.ConnectionPairPort
import com.opendatamask.domain.port.output.DataConnectionPort
import com.opendatamask.domain.port.output.WorkspacePort
import com.opendatamask.domain.model.ConnectionPair
import com.opendatamask.domain.model.ConnectionType
import com.opendatamask.domain.model.DataConnection
import com.opendatamask.domain.model.Workspace
import com.opendatamask.domain.port.input.dto.ConnectionPairRequest
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
class ConnectionPairServiceTest {

    @Mock private lateinit var connectionPairRepository: ConnectionPairPort
    @Mock private lateinit var dataConnectionRepository: DataConnectionPort
    @Mock private lateinit var workspaceRepository: WorkspacePort

    @InjectMocks
    private lateinit var service: ConnectionPairService

    private fun makeWorkspace(id: Long = 1L) = Workspace(
        id = id, name = "WS", ownerId = 1L,
        createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
    )

    private fun makeDataConnection(id: Long = 1L, workspaceId: Long = 1L) = DataConnection(
        id = id, workspaceId = workspaceId, name = "conn-$id",
        type = ConnectionType.POSTGRESQL, connectionString = "enc_conn",
        isSource = id == 1L, isDestination = id == 2L
    )

    private fun makeConnectionPair(
        id: Long = 10L,
        workspaceId: Long = 1L,
        deletedAt: LocalDateTime? = null
    ) = ConnectionPair(
        id = id, workspaceId = workspaceId,
        name = "Pair A", description = "Test pair",
        sourceConnectionId = 1L, destinationConnectionId = 2L,
        createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now(),
        deletedAt = deletedAt
    )

    private fun makeRequest(
        name: String = "Pair A",
        sourceConnectionId: Long = 1L,
        destinationConnectionId: Long = 2L
    ) = ConnectionPairRequest(
        name = name,
        description = "Test pair",
        sourceConnectionId = sourceConnectionId,
        destinationConnectionId = destinationConnectionId
    )

    // ── createConnectionPair ───────────────────────────────────────────────

    @Test
    fun `createConnectionPair saves pair and returns response`() {
        val saved = makeConnectionPair()
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(makeDataConnection(1L)))
        whenever(dataConnectionRepository.findById(2L)).thenReturn(Optional.of(makeDataConnection(2L)))
        whenever(connectionPairRepository.save(any<ConnectionPair>())).thenReturn(saved)

        val response = service.createConnectionPair(1L, makeRequest())

        assertEquals(10L, response.id)
        assertEquals(1L, response.workspaceId)
        assertEquals("Pair A", response.name)
        assertEquals(1L, response.sourceConnectionId)
        assertEquals(2L, response.destinationConnectionId)
        verify(connectionPairRepository).save(any())
    }

    @Test
    fun `createConnectionPair throws when workspace not found`() {
        whenever(workspaceRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.createConnectionPair(99L, makeRequest()) }
        verify(connectionPairRepository, never()).save(any())
    }

    @Test
    fun `createConnectionPair throws when source connection not found`() {
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.createConnectionPair(1L, makeRequest()) }
    }

    @Test
    fun `createConnectionPair throws when destination connection not found`() {
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(makeDataConnection(1L)))
        whenever(dataConnectionRepository.findById(2L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.createConnectionPair(1L, makeRequest()) }
    }

    @Test
    fun `createConnectionPair throws when source connection belongs to different workspace`() {
        val foreignConn = makeDataConnection(1L, workspaceId = 99L)
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(foreignConn))

        assertThrows<IllegalArgumentException> { service.createConnectionPair(1L, makeRequest()) }
    }

    @Test
    fun `createConnectionPair throws when destination connection belongs to different workspace`() {
        val foreignDest = makeDataConnection(2L, workspaceId = 99L)
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(makeDataConnection(1L)))
        whenever(dataConnectionRepository.findById(2L)).thenReturn(Optional.of(foreignDest))

        assertThrows<IllegalArgumentException> { service.createConnectionPair(1L, makeRequest()) }
    }

    // ── getConnectionPair ──────────────────────────────────────────────────

    @Test
    fun `getConnectionPair returns active pair for matching workspace`() {
        val pair = makeConnectionPair(id = 10L, workspaceId = 1L)
        whenever(connectionPairRepository.findById(10L)).thenReturn(Optional.of(pair))

        val response = service.getConnectionPair(1L, 10L)

        assertEquals(10L, response.id)
        assertEquals(1L, response.workspaceId)
    }

    @Test
    fun `getConnectionPair throws when pair not found`() {
        whenever(connectionPairRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.getConnectionPair(1L, 99L) }
    }

    @Test
    fun `getConnectionPair throws when pair belongs to different workspace`() {
        val pair = makeConnectionPair(id = 10L, workspaceId = 99L)
        whenever(connectionPairRepository.findById(10L)).thenReturn(Optional.of(pair))

        assertThrows<NoSuchElementException> { service.getConnectionPair(1L, 10L) }
    }

    @Test
    fun `getConnectionPair throws when pair is soft-deleted`() {
        val pair = makeConnectionPair(id = 10L, deletedAt = LocalDateTime.now())
        whenever(connectionPairRepository.findById(10L)).thenReturn(Optional.of(pair))

        assertThrows<NoSuchElementException> { service.getConnectionPair(1L, 10L) }
    }

    // ── listConnectionPairs ────────────────────────────────────────────────

    @Test
    fun `listConnectionPairs returns only active pairs for workspace`() {
        val pairs = listOf(makeConnectionPair(id = 10L), makeConnectionPair(id = 11L))
        whenever(connectionPairRepository.findActiveByWorkspaceId(1L)).thenReturn(pairs)

        val result = service.listConnectionPairs(1L)

        assertEquals(2, result.size)
    }

    @Test
    fun `listConnectionPairs returns empty list when none found`() {
        whenever(connectionPairRepository.findActiveByWorkspaceId(1L)).thenReturn(emptyList())

        val result = service.listConnectionPairs(1L)

        assertTrue(result.isEmpty())
    }

    // ── updateConnectionPair ───────────────────────────────────────────────

    @Test
    fun `updateConnectionPair updates fields and saves`() {
        val pair = makeConnectionPair(id = 10L, workspaceId = 1L)
        val updateRequest = makeRequest(name = "Updated Pair")
        whenever(connectionPairRepository.findById(10L)).thenReturn(Optional.of(pair))
        whenever(dataConnectionRepository.findById(1L)).thenReturn(Optional.of(makeDataConnection(1L)))
        whenever(dataConnectionRepository.findById(2L)).thenReturn(Optional.of(makeDataConnection(2L)))
        whenever(connectionPairRepository.save(any<ConnectionPair>())).thenAnswer { it.arguments[0] as ConnectionPair }

        val response = service.updateConnectionPair(1L, 10L, updateRequest)

        assertEquals("Updated Pair", response.name)
        verify(connectionPairRepository).save(any())
    }

    @Test
    fun `updateConnectionPair throws when pair not found`() {
        whenever(connectionPairRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.updateConnectionPair(1L, 99L, makeRequest()) }
    }

    // ── deleteConnectionPair ───────────────────────────────────────────────

    @Test
    fun `deleteConnectionPair sets deletedAt for soft delete`() {
        val pair = makeConnectionPair(id = 10L, workspaceId = 1L)
        whenever(connectionPairRepository.findById(10L)).thenReturn(Optional.of(pair))
        whenever(connectionPairRepository.save(any<ConnectionPair>())).thenAnswer { it.arguments[0] as ConnectionPair }

        service.deleteConnectionPair(1L, 10L)

        val captor = argumentCaptor<ConnectionPair>()
        verify(connectionPairRepository).save(captor.capture())
        assertNotNull(captor.firstValue.deletedAt)
    }

    @Test
    fun `deleteConnectionPair throws when pair not found`() {
        whenever(connectionPairRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.deleteConnectionPair(1L, 99L) }
    }

    @Test
    fun `deleteConnectionPair throws when pair already deleted`() {
        val deletedPair = makeConnectionPair(id = 10L, deletedAt = LocalDateTime.now())
        whenever(connectionPairRepository.findById(10L)).thenReturn(Optional.of(deletedPair))

        assertThrows<NoSuchElementException> { service.deleteConnectionPair(1L, 10L) }
    }
}
