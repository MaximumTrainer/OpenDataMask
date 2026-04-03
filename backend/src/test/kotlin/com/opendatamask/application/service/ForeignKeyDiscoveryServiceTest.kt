package com.opendatamask.application.service

import com.opendatamask.domain.port.output.ConnectorFactoryPort
import com.opendatamask.domain.port.output.DatabaseConnector
import com.opendatamask.domain.port.output.ForeignKeyInfo
import com.opendatamask.domain.model.ConnectionType
import com.opendatamask.domain.model.DataConnection
import com.opendatamask.domain.model.ForeignKeyRelationship
import com.opendatamask.domain.port.output.DataConnectionPort
import com.opendatamask.domain.port.output.ForeignKeyRelationshipPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.test.context.ActiveProfiles
import java.util.Optional

@ActiveProfiles("test")
class ForeignKeyDiscoveryServiceTest {

    private val fkRepo = mock<ForeignKeyRelationshipPort>()
    private val dataConnectionRepo = mock<DataConnectionPort>()
    private val connectorFactory = mock<ConnectorFactoryPort>()

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
        whenever(fkRepo.save(any<ForeignKeyRelationship>())).thenAnswer { it.arguments[0] as ForeignKeyRelationship }

        service.discoverForeignKeys(1L)

        verify(fkRepo, atLeastOnce()).save(argThat<ForeignKeyRelationship> { fk ->
            fk.fromTable == "orders" && fk.fromColumn == "customer_id" && fk.toTable == "customers"
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
        whenever(fkRepo.save(any<ForeignKeyRelationship>())).thenAnswer { it.arguments[0] as ForeignKeyRelationship }

        service.discoverForeignKeys(1L)

        // Should not save because FK already exists
        verify(fkRepo, never()).save(argThat<ForeignKeyRelationship> { fk -> fk.fromTable == "orders" })
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
