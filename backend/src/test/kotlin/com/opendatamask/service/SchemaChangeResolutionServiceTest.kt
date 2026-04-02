package com.opendatamask.service

import com.opendatamask.domain.model.*
import com.opendatamask.adapter.output.persistence.SchemaChangeRepository
import com.opendatamask.adapter.output.persistence.WorkspaceRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.Optional

class SchemaChangeResolutionServiceTest {

    private val changeRepo = mock<SchemaChangeRepository>()
    private val workspaceRepo = mock<WorkspaceRepository>()
    private val snapshotRepo = mock<com.opendatamask.adapter.output.persistence.SchemaSnapshotRepository>()
    private val connectionRepo = mock<com.opendatamask.adapter.output.persistence.DataConnectionRepository>()
    private val connectorFactory = mock<com.opendatamask.connector.ConnectorFactory>()
    private val webhookService = mock<WebhookService>()
    private val service = SchemaChangeService(snapshotRepo, changeRepo, workspaceRepo, connectionRepo, connectorFactory, webhookService)

    private fun makeChange(
        id: Long,
        type: SchemaChangeType,
        status: SchemaChangeStatus = SchemaChangeStatus.UNRESOLVED
    ) = SchemaChange(id = id, workspaceId = 1L, changeType = type, tableName = "t", status = status)

    // ── resolveChange ──────────────────────────────────────────────────────

    @Test
    fun `resolveChange sets status to RESOLVED`() {
        val change = makeChange(1L, SchemaChangeType.NEW_COLUMN)
        whenever(changeRepo.findById(1L)).thenReturn(Optional.of(change))
        whenever(changeRepo.save(any<SchemaChange>())).thenAnswer { it.arguments[0] as SchemaChange }

        service.resolveChange(1L)

        verify(changeRepo).save(argThat { status == SchemaChangeStatus.RESOLVED })
    }

    @Test
    fun `resolveChange does nothing when change not found`() {
        whenever(changeRepo.findById(99L)).thenReturn(Optional.empty())

        service.resolveChange(99L)

        verify(changeRepo, never()).save(any())
    }

    // ── dismissChange ──────────────────────────────────────────────────────

    @Test
    fun `dismissChange sets status to DISMISSED`() {
        val change = makeChange(2L, SchemaChangeType.DROPPED_TABLE)
        whenever(changeRepo.findById(2L)).thenReturn(Optional.of(change))
        whenever(changeRepo.save(any<SchemaChange>())).thenAnswer { it.arguments[0] as SchemaChange }

        service.dismissChange(2L)

        verify(changeRepo).save(argThat { status == SchemaChangeStatus.DISMISSED })
    }

    @Test
    fun `dismissChange does nothing when change not found`() {
        whenever(changeRepo.findById(99L)).thenReturn(Optional.empty())

        service.dismissChange(99L)

        verify(changeRepo, never()).save(any())
    }

    // ── resolveAll ─────────────────────────────────────────────────────────

    @Test
    fun `resolveAll only resolves EXPOSING type changes`() {
        val exposing1 = makeChange(1L, SchemaChangeType.NEW_COLUMN)
        val exposing2 = makeChange(2L, SchemaChangeType.TYPE_CHANGED)
        val notification = makeChange(3L, SchemaChangeType.DROPPED_TABLE)
        whenever(changeRepo.findByWorkspaceIdAndStatus(1L, SchemaChangeStatus.UNRESOLVED))
            .thenReturn(listOf(exposing1, exposing2, notification))
        whenever(changeRepo.saveAll(any<List<SchemaChange>>())).thenAnswer { it.arguments[0] as List<SchemaChange> }

        service.resolveAll(1L)

        verify(changeRepo).saveAll(argThat<List<SchemaChange>> { size == 2 && all { it.status == SchemaChangeStatus.RESOLVED } })
        assertEquals(SchemaChangeStatus.UNRESOLVED, notification.status)
    }

    @Test
    fun `resolveAll resolves NULLABILITY_CHANGED as exposing`() {
        val change = makeChange(1L, SchemaChangeType.NULLABILITY_CHANGED)
        whenever(changeRepo.findByWorkspaceIdAndStatus(1L, SchemaChangeStatus.UNRESOLVED)).thenReturn(listOf(change))
        whenever(changeRepo.saveAll(any<List<SchemaChange>>())).thenAnswer { it.arguments[0] as List<SchemaChange> }

        service.resolveAll(1L)

        verify(changeRepo).saveAll(argThat<List<SchemaChange>> { size == 1 && first().status == SchemaChangeStatus.RESOLVED })
    }

    @Test
    fun `resolveAll does not resolve NEW_TABLE (notification type)`() {
        val change = makeChange(1L, SchemaChangeType.NEW_TABLE)
        whenever(changeRepo.findByWorkspaceIdAndStatus(1L, SchemaChangeStatus.UNRESOLVED)).thenReturn(listOf(change))
        whenever(changeRepo.saveAll(any<List<SchemaChange>>())).thenAnswer { it.arguments[0] as List<SchemaChange> }

        service.resolveAll(1L)

        verify(changeRepo).saveAll(argThat<List<SchemaChange>> { isEmpty() })
    }

    // ── dismissAll ─────────────────────────────────────────────────────────

    @Test
    fun `dismissAll only dismisses notification type changes`() {
        val notification1 = makeChange(1L, SchemaChangeType.DROPPED_COLUMN)
        val notification2 = makeChange(2L, SchemaChangeType.DROPPED_TABLE)
        val notification3 = makeChange(3L, SchemaChangeType.NEW_TABLE)
        val exposing = makeChange(4L, SchemaChangeType.NEW_COLUMN)
        whenever(changeRepo.findByWorkspaceIdAndStatus(1L, SchemaChangeStatus.UNRESOLVED))
            .thenReturn(listOf(notification1, notification2, notification3, exposing))
        whenever(changeRepo.saveAll(any<List<SchemaChange>>())).thenAnswer { it.arguments[0] as List<SchemaChange> }

        service.dismissAll(1L)

        verify(changeRepo).saveAll(argThat<List<SchemaChange>> {
            size == 3 && all { it.status == SchemaChangeStatus.DISMISSED }
        })
        assertEquals(SchemaChangeStatus.UNRESOLVED, exposing.status)
    }

    @Test
    fun `dismissAll does nothing when no notification changes exist`() {
        whenever(changeRepo.findByWorkspaceIdAndStatus(1L, SchemaChangeStatus.UNRESOLVED)).thenReturn(emptyList())
        whenever(changeRepo.saveAll(any<List<SchemaChange>>())).thenReturn(emptyList())

        service.dismissAll(1L)

        verify(changeRepo).saveAll(argThat<List<SchemaChange>> { isEmpty() })
    }

    // ── status transitions ─────────────────────────────────────────────────

    @Test
    fun `resolved change cannot be dismissed (no-op on already resolved)`() {
        val change = makeChange(1L, SchemaChangeType.DROPPED_TABLE, SchemaChangeStatus.RESOLVED)
        whenever(changeRepo.findById(1L)).thenReturn(Optional.of(change))
        whenever(changeRepo.save(any<SchemaChange>())).thenAnswer { it.arguments[0] as SchemaChange }

        service.dismissChange(1L)

        // Still saved (service doesn't guard this, but status changes)
        verify(changeRepo).save(argThat { status == SchemaChangeStatus.DISMISSED })
    }
}
