package com.opendatamask.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opendatamask.connector.ColumnInfo
import com.opendatamask.connector.ConnectorFactory
import com.opendatamask.connector.DatabaseConnector
import com.opendatamask.model.*
import com.opendatamask.repository.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import java.util.Optional

class SchemaChangeServiceTest {

    private val snapshotRepo = mock<SchemaSnapshotRepository>()
    private val changeRepo = mock<SchemaChangeRepository>()
    private val workspaceRepo = mock<WorkspaceRepository>()
    private val connectionRepo = mock<DataConnectionRepository>()
    private val connectorFactory = mock<ConnectorFactory>()
    private val service = SchemaChangeService(snapshotRepo, changeRepo, workspaceRepo, connectionRepo, connectorFactory)
    private val mapper = jacksonObjectMapper()

    @Test
    fun `detectChanges returns empty when no source connection`() {
        val workspace = Workspace(id = 1L, name = "test", ownerId = 1L)
        whenever(workspaceRepo.findById(1L)).thenReturn(Optional.of(workspace))
        whenever(connectionRepo.findByWorkspaceIdAndIsSource(1L, true)).thenReturn(emptyList())
        val changes = service.detectChanges(1L)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `detectChanges detects new column`() {
        val workspace = Workspace(id = 1L, name = "test", ownerId = 1L)
        val conn = DataConnection(id = 1L, workspaceId = 1L, name = "src", type = ConnectionType.POSTGRESQL, connectionString = "jdbc:h2:mem:test", isSource = true)
        val connector = mock<DatabaseConnector>()

        val prevSchema = WorkspaceSchema(listOf(
            TableSchema("users", listOf(ColumnInfo("id", "INT", false), ColumnInfo("name", "VARCHAR", true)))
        ))
        val snapshot = SchemaSnapshot(workspaceId = 1L, schemaJson = mapper.writeValueAsString(prevSchema))

        whenever(workspaceRepo.findById(1L)).thenReturn(Optional.of(workspace))
        whenever(connectionRepo.findByWorkspaceIdAndIsSource(1L, true)).thenReturn(listOf(conn))
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(connector)
        whenever(connector.listTables()).thenReturn(listOf("users"))
        whenever(connector.listColumns("users")).thenReturn(listOf(
            ColumnInfo("id", "INT", false), ColumnInfo("name", "VARCHAR", true), ColumnInfo("email", "VARCHAR", true)
        ))
        whenever(snapshotRepo.findTopByWorkspaceIdOrderBySnapshotAtDesc(1L)).thenReturn(snapshot)
        whenever(changeRepo.findByWorkspaceIdAndStatus(1L, SchemaChangeStatus.UNRESOLVED)).thenReturn(emptyList())
        whenever(changeRepo.saveAll(any<List<SchemaChange>>())).thenAnswer { it.arguments[0] as List<SchemaChange> }
        whenever(snapshotRepo.save(any<SchemaSnapshot>())).thenAnswer { it.arguments[0] as SchemaSnapshot }

        val changes = service.detectChanges(1L)
        assertTrue(changes.any { it.changeType == SchemaChangeType.NEW_COLUMN && it.columnName == "email" })
    }

    @Test
    fun `isBlockingJobRun returns false when NEVER_BLOCK`() {
        val workspace = Workspace(id = 1L, name = "test", ownerId = 1L, schemaChangeHandling = SchemaChangeHandling.NEVER_BLOCK)
        whenever(workspaceRepo.findById(1L)).thenReturn(Optional.of(workspace))
        whenever(changeRepo.findByWorkspaceIdAndStatus(1L, SchemaChangeStatus.UNRESOLVED)).thenReturn(
            listOf(SchemaChange(workspaceId = 1L, changeType = SchemaChangeType.NEW_COLUMN, tableName = "t"))
        )
        assertFalse(service.isBlockingJobRun(1L))
    }

    @Test
    fun `isBlockingJobRun returns true when BLOCK_EXPOSING and NEW_COLUMN exists`() {
        val workspace = Workspace(id = 1L, name = "test", ownerId = 1L, schemaChangeHandling = SchemaChangeHandling.BLOCK_EXPOSING)
        whenever(workspaceRepo.findById(1L)).thenReturn(Optional.of(workspace))
        whenever(changeRepo.findByWorkspaceIdAndStatus(1L, SchemaChangeStatus.UNRESOLVED)).thenReturn(
            listOf(SchemaChange(workspaceId = 1L, changeType = SchemaChangeType.NEW_COLUMN, tableName = "t", columnName = "c"))
        )
        assertTrue(service.isBlockingJobRun(1L))
    }

    @Test
    fun `resolveChange updates status to RESOLVED`() {
        val change = SchemaChange(id = 1L, workspaceId = 1L, changeType = SchemaChangeType.NEW_COLUMN, tableName = "t")
        whenever(changeRepo.findById(1L)).thenReturn(Optional.of(change))
        service.resolveChange(1L)
        verify(changeRepo).save(argThat { status == SchemaChangeStatus.RESOLVED })
    }
}
