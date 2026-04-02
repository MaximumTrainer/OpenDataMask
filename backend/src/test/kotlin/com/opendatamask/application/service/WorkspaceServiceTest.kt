package com.opendatamask.application.service

import com.opendatamask.adapter.input.rest.dto.RegisterRequest
import com.opendatamask.adapter.input.rest.dto.WorkspaceRequest
import com.opendatamask.adapter.input.rest.dto.WorkspaceUserRequest
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.adapter.output.persistence.WorkspaceRepository
import com.opendatamask.adapter.output.persistence.WorkspaceUserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkspaceServiceTest {

    @Autowired
    private lateinit var workspaceService: WorkspaceService

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var workspaceRepository: WorkspaceRepository

    @Autowired
    private lateinit var workspaceUserRepository: WorkspaceUserRepository

    private var ownerId: Long = 0
    private var secondUserId: Long = 0

    @BeforeEach
    fun setup() {
        authService.register(
            RegisterRequest(
                username = "wsowner_${System.nanoTime()}",
                email = "wsowner_${System.nanoTime()}@example.com",
                password = "password123"
            )
        )
        val owner = userRepository.findAll().last { it.username.startsWith("wsowner") }
        ownerId = owner.id

        authService.register(
            RegisterRequest(
                username = "wsuser_${System.nanoTime()}",
                email = "wsuser_${System.nanoTime()}@example.com",
                password = "password123"
            )
        )
        val secondUser = userRepository.findAll().last { it.username.startsWith("wsuser") }
        secondUserId = secondUser.id
    }

    @Test
    fun `createWorkspace saves workspace and adds owner as admin`() {
        val request = WorkspaceRequest(name = "Test Workspace", description = "A test workspace")
        val response = workspaceService.createWorkspace(request, ownerId)

        assertNotNull(response.id)
        assertEquals("Test Workspace", response.name)
        assertEquals("A test workspace", response.description)
        assertEquals(ownerId, response.ownerId)

        val members = workspaceUserRepository.findByWorkspaceId(response.id)
        assertEquals(1, members.size)
        assertEquals(ownerId, members[0].userId)
        assertEquals(com.opendatamask.domain.model.WorkspaceRole.ADMIN, members[0].role)
    }

    @Test
    fun `getWorkspace returns workspace by id`() {
        val request = WorkspaceRequest(name = "Get Workspace Test")
        val created = workspaceService.createWorkspace(request, ownerId)

        val fetched = workspaceService.getWorkspace(created.id)
        assertEquals(created.id, fetched.id)
        assertEquals("Get Workspace Test", fetched.name)
    }

    @Test
    fun `getWorkspace throws for non-existent id`() {
        assertThrows<NoSuchElementException> {
            workspaceService.getWorkspace(999999L)
        }
    }

    @Test
    fun `listWorkspaces returns workspaces for user`() {
        workspaceService.createWorkspace(WorkspaceRequest(name = "WS 1"), ownerId)
        workspaceService.createWorkspace(WorkspaceRequest(name = "WS 2"), ownerId)

        val workspaces = workspaceService.listWorkspaces(ownerId)
        assertTrue(workspaces.size >= 2)
        assertTrue(workspaces.any { it.name == "WS 1" })
        assertTrue(workspaces.any { it.name == "WS 2" })
    }

    @Test
    fun `updateWorkspace changes workspace properties`() {
        val created = workspaceService.createWorkspace(
            WorkspaceRequest(name = "Original Name"),
            ownerId
        )

        val updated = workspaceService.updateWorkspace(
            created.id,
            WorkspaceRequest(name = "Updated Name", description = "New description"),
            ownerId
        )

        assertEquals("Updated Name", updated.name)
        assertEquals("New description", updated.description)
    }

    @Test
    fun `updateWorkspace throws for non-admin user`() {
        val created = workspaceService.createWorkspace(
            WorkspaceRequest(name = "Admin Only Workspace"),
            ownerId
        )

        assertThrows<SecurityException> {
            workspaceService.updateWorkspace(
                created.id,
                WorkspaceRequest(name = "Hacked"),
                secondUserId
            )
        }
    }

    @Test
    fun `deleteWorkspace removes workspace`() {
        val created = workspaceService.createWorkspace(
            WorkspaceRequest(name = "To Delete"),
            ownerId
        )

        workspaceService.deleteWorkspace(created.id, ownerId)

        assertFalse(workspaceRepository.existsById(created.id))
    }

    @Test
    fun `addUserToWorkspace adds user with given role`() {
        val ws = workspaceService.createWorkspace(WorkspaceRequest(name = "Multi-user WS"), ownerId)

        val addRequest = WorkspaceUserRequest(userId = secondUserId, role = "USER")
        val result = workspaceService.addUserToWorkspace(ws.id, addRequest, ownerId)

        assertEquals(secondUserId, result.userId)
        assertEquals("USER", result.role)
    }

    @Test
    fun `getUsersInWorkspace returns all workspace members`() {
        val ws = workspaceService.createWorkspace(WorkspaceRequest(name = "Members WS"), ownerId)
        workspaceService.addUserToWorkspace(ws.id, WorkspaceUserRequest(userId = secondUserId, role = "VIEWER"), ownerId)

        val users = workspaceService.getUsersInWorkspace(ws.id)
        assertEquals(2, users.size)
    }

    @Test
    fun `removeUserFromWorkspace removes the user`() {
        val ws = workspaceService.createWorkspace(WorkspaceRequest(name = "Remove User WS"), ownerId)
        workspaceService.addUserToWorkspace(ws.id, WorkspaceUserRequest(userId = secondUserId, role = "USER"), ownerId)

        workspaceService.removeUserFromWorkspace(ws.id, secondUserId, ownerId)

        val users = workspaceService.getUsersInWorkspace(ws.id)
        assertFalse(users.any { it.userId == secondUserId })
    }
}
