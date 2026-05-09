package com.opendatamask.application.service

import com.opendatamask.domain.model.AuditAction
import com.opendatamask.domain.model.AuditLog
import com.opendatamask.domain.model.AuditResourceType
import com.opendatamask.domain.port.output.AuditLogPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class AuditServiceTest {

    @Mock private lateinit var auditLogPort: AuditLogPort
    @InjectMocks private lateinit var auditService: AuditService

    @Test
    fun `should save audit log entry when record is called`() {
        val saved = makeAuditLog()
        whenever(auditLogPort.save(any())).thenReturn(saved)

        auditService.record(
            workspaceId = 1L,
            action = AuditAction.JOB_STARTED,
            resourceType = AuditResourceType.JOB,
            resourceId = "42",
            actorId = 99L,
            actorUsername = "alice"
        )

        verify(auditLogPort).save(argThat { entry ->
            entry.workspaceId == 1L &&
            entry.action == AuditAction.JOB_STARTED &&
            entry.resourceType == AuditResourceType.JOB &&
            entry.resourceId == "42" &&
            entry.actorId == 99L &&
            entry.actorUsername == "alice"
        })
    }

    @Test
    fun `should persist description and IP address in audit log`() {
        whenever(auditLogPort.save(any())).thenAnswer { it.arguments[0] as AuditLog }

        auditService.record(
            workspaceId = 2L,
            action = AuditAction.CONNECTION_CREATED,
            resourceType = AuditResourceType.CONNECTION,
            ipAddress = "192.168.1.1",
            description = "Created postgres connection"
        )

        verify(auditLogPort).save(argThat { entry ->
            entry.ipAddress == "192.168.1.1" && entry.description == "Created postgres connection"
        })
    }

    @Test
    fun `should return workspace audit log entries mapped to response DTOs`() {
        val entries = listOf(makeAuditLog(id = 1L), makeAuditLog(id = 2L))
        whenever(auditLogPort.findByWorkspaceId(eq(1L), isNull(), isNull(), any())).thenReturn(entries)

        val result = auditService.getWorkspaceAuditLog(workspaceId = 1L)

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
        assertEquals(AuditAction.JOB_STARTED, result[0].action)
    }

    @Test
    fun `should return global audit log entries mapped to response DTOs`() {
        val entries = listOf(makeAuditLog(id = 5L))
        whenever(auditLogPort.findAll(isNull(), isNull(), any())).thenReturn(entries)

        val result = auditService.getGlobalAuditLog()

        assertEquals(1, result.size)
        assertEquals(5L, result[0].id)
    }

    @Test
    fun `should cap limit at 1000 for workspace audit log`() {
        whenever(auditLogPort.findByWorkspaceId(any(), isNull(), isNull(), eq(1000))).thenReturn(emptyList())

        auditService.getWorkspaceAuditLog(workspaceId = 1L, limit = 9999)

        verify(auditLogPort).findByWorkspaceId(eq(1L), isNull(), isNull(), eq(1000))
    }

    @Test
    fun `should cap limit at 1000 for global audit log`() {
        whenever(auditLogPort.findAll(isNull(), isNull(), eq(1000))).thenReturn(emptyList())

        auditService.getGlobalAuditLog(limit = 5000)

        verify(auditLogPort).findAll(isNull(), isNull(), eq(1000))
    }

    @Test
    fun `should pass date range filters through to port`() {
        val from = Instant.parse("2024-01-01T00:00:00Z")
        val to = Instant.parse("2024-12-31T23:59:59Z")
        whenever(auditLogPort.findByWorkspaceId(any(), eq(from), eq(to), any())).thenReturn(emptyList())

        auditService.getWorkspaceAuditLog(workspaceId = 1L, from = from, to = to)

        verify(auditLogPort).findByWorkspaceId(eq(1L), eq(from), eq(to), any())
    }

    private fun makeAuditLog(id: Long = 1L) = AuditLog(
        id = id,
        timestamp = Instant.now(),
        actorId = 1L,
        actorUsername = "testuser",
        action = AuditAction.JOB_STARTED,
        resourceType = AuditResourceType.JOB,
        resourceId = "42",
        workspaceId = 1L
    )
}
