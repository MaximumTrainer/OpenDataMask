package com.opendatamask.application.service

import com.opendatamask.domain.model.WorkspacePermission
import com.opendatamask.domain.model.WorkspaceRole
import com.opendatamask.domain.model.WorkspaceUser
import com.opendatamask.domain.model.WorkspaceUserPermission
import com.opendatamask.adapter.output.persistence.WorkspaceUserPermissionRepository
import com.opendatamask.adapter.output.persistence.WorkspaceUserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.ActiveProfiles
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@ActiveProfiles("test")
class PermissionServiceTest {

    @Mock
    private lateinit var workspaceUserRepository: WorkspaceUserRepository

    @Mock
    private lateinit var workspaceUserPermissionRepository: WorkspaceUserPermissionRepository

    @InjectMocks
    private lateinit var permissionService: PermissionService

    private val workspaceId = 1L
    private val userId = 10L
    private val workspaceUserId = 100L

    private fun makeWorkspaceUser(role: WorkspaceRole) =
        WorkspaceUser(id = workspaceUserId, workspaceId = workspaceId, userId = userId, role = role)

    @Test
    fun `ADMIN gets all permissions by default`() {
        whenever(workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
            .thenReturn(Optional.of(makeWorkspaceUser(WorkspaceRole.ADMIN)))
        whenever(workspaceUserPermissionRepository.findByWorkspaceUserId(workspaceUserId))
            .thenReturn(emptyList())

        val permissions = permissionService.getEffectivePermissions(userId, workspaceId)

        assertEquals(WorkspacePermission.values().toSet(), permissions)
    }

    @Test
    fun `USER gets exactly CONFIGURE_GENERATORS PREVIEW_SOURCE_DATA PREVIEW_DESTINATION_DATA RUN_JOBS`() {
        whenever(workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
            .thenReturn(Optional.of(makeWorkspaceUser(WorkspaceRole.USER)))
        whenever(workspaceUserPermissionRepository.findByWorkspaceUserId(workspaceUserId))
            .thenReturn(emptyList())

        val permissions = permissionService.getEffectivePermissions(userId, workspaceId)

        assertEquals(
            setOf(
                WorkspacePermission.CONFIGURE_GENERATORS,
                WorkspacePermission.PREVIEW_SOURCE_DATA,
                WorkspacePermission.PREVIEW_DESTINATION_DATA,
                WorkspacePermission.RUN_JOBS
            ),
            permissions
        )
    }

    @Test
    fun `VIEWER gets exactly PREVIEW_SOURCE_DATA PREVIEW_DESTINATION_DATA`() {
        whenever(workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
            .thenReturn(Optional.of(makeWorkspaceUser(WorkspaceRole.VIEWER)))
        whenever(workspaceUserPermissionRepository.findByWorkspaceUserId(workspaceUserId))
            .thenReturn(emptyList())

        val permissions = permissionService.getEffectivePermissions(userId, workspaceId)

        assertEquals(
            setOf(
                WorkspacePermission.PREVIEW_SOURCE_DATA,
                WorkspacePermission.PREVIEW_DESTINATION_DATA
            ),
            permissions
        )
    }

    @Test
    fun `custom grant adds to USER's permission set`() {
        whenever(workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
            .thenReturn(Optional.of(makeWorkspaceUser(WorkspaceRole.USER)))
        whenever(workspaceUserPermissionRepository.findByWorkspaceUserId(workspaceUserId))
            .thenReturn(
                listOf(
                    WorkspaceUserPermission(
                        workspaceUserId = workspaceUserId,
                        permission = WorkspacePermission.CONFIGURE_SENSITIVITY,
                        granted = true
                    )
                )
            )

        val permissions = permissionService.getEffectivePermissions(userId, workspaceId)

        assertTrue(permissions.contains(WorkspacePermission.CONFIGURE_SENSITIVITY))
        assertTrue(permissions.contains(WorkspacePermission.CONFIGURE_GENERATORS))
        assertTrue(permissions.contains(WorkspacePermission.RUN_JOBS))
    }

    @Test
    fun `explicit revocation removes from USER's permission set`() {
        whenever(workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
            .thenReturn(Optional.of(makeWorkspaceUser(WorkspaceRole.USER)))
        whenever(workspaceUserPermissionRepository.findByWorkspaceUserId(workspaceUserId))
            .thenReturn(
                listOf(
                    WorkspaceUserPermission(
                        workspaceUserId = workspaceUserId,
                        permission = WorkspacePermission.RUN_JOBS,
                        granted = false
                    )
                )
            )

        val permissions = permissionService.getEffectivePermissions(userId, workspaceId)

        assertFalse(permissions.contains(WorkspacePermission.RUN_JOBS))
        assertTrue(permissions.contains(WorkspacePermission.CONFIGURE_GENERATORS))
    }

    @Test
    fun `requirePermission passes when permission is held`() {
        whenever(workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
            .thenReturn(Optional.of(makeWorkspaceUser(WorkspaceRole.USER)))
        whenever(workspaceUserPermissionRepository.findByWorkspaceUserId(workspaceUserId))
            .thenReturn(emptyList())

        assertDoesNotThrow {
            permissionService.requirePermission(userId, workspaceId, WorkspacePermission.RUN_JOBS)
        }
    }

    @Test
    fun `requirePermission throws AccessDeniedException when permission is not held`() {
        whenever(workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
            .thenReturn(Optional.of(makeWorkspaceUser(WorkspaceRole.VIEWER)))
        whenever(workspaceUserPermissionRepository.findByWorkspaceUserId(workspaceUserId))
            .thenReturn(emptyList())

        assertThrows<AccessDeniedException> {
            permissionService.requirePermission(userId, workspaceId, WorkspacePermission.RUN_JOBS)
        }
    }
}
