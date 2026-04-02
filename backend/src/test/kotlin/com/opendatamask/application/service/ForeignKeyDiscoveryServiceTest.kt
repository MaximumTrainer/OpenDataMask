package com.opendatamask.application.service

import com.opendatamask.adapter.output.connector.ConnectorFactory
import com.opendatamask.adapter.output.connector.DatabaseConnector
import com.opendatamask.adapter.output.connector.ForeignKeyInfo
import com.opendatamask.domain.model.ConnectionType
import com.opendatamask.domain.model.DataConnection
import com.opendatamask.domain.model.ForeignKeyRelationship
import com.opendatamask.adapter.output.persistence.DataConnectionRepository
import com.opendatamask.adapter.output.persistence.ForeignKeyRelationshipRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.test.context.ActiveProfiles
import java.util.Optional

@ActiveProfiles("test")
class ForeignKeyDiscoveryServiceTest {

    private val fkRepo = mock<ForeignKeyRelationshipRepository>()
    private val dataConnectionRepo = mock<DataConnectionRepository>()
    private val connectorFactory = mock<ConnectorFactory>()

    private val service = ForeignKeyDiscoveryService(fkRepo, dataConnectionRepo, connectorFactory)

    private fun makeConnection(id: Long = 1L) = DataConnection(
        id = id, workspaceId = 1L, name = "src", type = ConnectionType.POSTGRESQL,
        connectionString = "jdbc:postgresql://localhost/db", isSource = true
    )

    @Test
    fun `discoverForeignKeys returns existing FKs when no source connection`() {
        val existing = listOf(
            ForeignKeyRelationship(workspaceId = 1L, fromTable = "orders", fromColumn = "customer_id", toTable = "customers", toColumn = "id")
        )
        whenever(dataConnectionRepo.findByWorkspaceIdAndIsSource(1L, true)).thenReturn(emptyList())
        whenever(fkRepo.findByWorkspaceId(1L)).thenReturn(existing)

        val result = service.discoverForeignKeys(1L)
        assertEquals(1, result.size)
        verify(connectorFactory, never()).createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `discoverForeignKeys persists discovered FK metadata`() {
        val conn = makeConnection()
        val connector = mock<DatabaseConnector>()

        val fkInfo = ForeignKeyInfo(fromTable = "orders", fromColumn = "customer_id", toTable = "customers", toColumn = "id")
        whenever(dataConnectionRepo.findByWorkspaceIdAndIsSource(1L, true)).thenReturn(listOf(conn))
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(connector)
        whenever(connector.listTables()).thenReturn(listOf("orders", "customers"))
        whenever(connector.listForeignKeys("orders")).thenReturn(listOf(fkInfo))
        whenever(connector.listForeignKeys("customers")).thenReturn(emptyList())
        whenever(fkRepo.findByWorkspaceId(1L)).thenReturn(emptyList())
        whenever(fkRepo.saveAll(any<List<ForeignKeyRelationship>>())).thenAnswer { it.arguments[0] as List<ForeignKeyRelationship> }

        service.discoverForeignKeys(1L)

        verify(fkRepo).saveAll(argThat<List<ForeignKeyRelationship>> { fks ->
            fks.any { it.fromTable == "orders" && it.fromColumn == "customer_id" && it.toTable == "customers" }
        })
    }

    @Test
    fun `discoverForeignKeys does not duplicate existing FKs`() {
        val conn = makeConnection()
        val connector = mock<DatabaseConnector>()

        val existing = ForeignKeyRelationship(
            workspaceId = 1L, fromTable = "orders", fromColumn = "customer_id",
            toTable = "customers", toColumn = "id", isVirtual = false
        )
        val fkInfo = ForeignKeyInfo("orders", "customer_id", "customers", "id")

        whenever(dataConnectionRepo.findByWorkspaceIdAndIsSource(1L, true)).thenReturn(listOf(conn))
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(connector)
        whenever(connector.listTables()).thenReturn(listOf("orders"))
        whenever(connector.listForeignKeys("orders")).thenReturn(listOf(fkInfo))
        whenever(fkRepo.findByWorkspaceId(1L)).thenReturn(listOf(existing))
        whenever(fkRepo.saveAll(any<List<ForeignKeyRelationship>>())).thenAnswer { it.arguments[0] as List<ForeignKeyRelationship> }

        service.discoverForeignKeys(1L)

        // Should not saveAll because FK already exists
        verify(fkRepo, never()).saveAll(argThat<List<ForeignKeyRelationship>> { list -> list.isNotEmpty() })
    }

    @Test
    fun `createVirtualForeignKey saves with isVirtual=true`() {
        val savedFk = ForeignKeyRelationship(
            workspaceId = 1L, fromTable = "orders", fromColumn = "user_id",
            toTable = "users", toColumn = "id", isVirtual = true
        )
        whenever(fkRepo.save(any<ForeignKeyRelationship>())).thenReturn(savedFk)

        val result = service.createVirtualForeignKey(1L, "orders", "user_id", "users", "id")

        assertTrue(result.isVirtual)
        verify(fkRepo).save(argThat<ForeignKeyRelationship> { isVirtual })
    }

    @Test
    fun `deleteForeignKey throws when FK does not belong to workspace`() {
        val fk = ForeignKeyRelationship(
            id = 1L, workspaceId = 2L, fromTable = "t", fromColumn = "c", toTable = "t2", toColumn = "id"
        )
        whenever(fkRepo.findById(1L)).thenReturn(Optional.of(fk))

        assertThrows(NoSuchElementException::class.java) {
            service.deleteForeignKey(workspaceId = 1L, fkId = 1L)
        }
    }
}
