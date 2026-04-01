package com.opendatamask.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.opendatamask.dto.*
import com.opendatamask.model.InheritedConfig
import com.opendatamask.model.Workspace
import com.opendatamask.repository.UserRepository
import com.opendatamask.security.JwtTokenProvider
import com.opendatamask.security.UserDetailsServiceImpl
import com.opendatamask.service.WorkspaceInheritanceService
import com.opendatamask.service.WorkspaceService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.time.LocalDateTime

@WebMvcTest(
    WorkspaceInheritanceController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class]
)
@ActiveProfiles("test")
class WorkspaceInheritanceControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockBean private lateinit var workspaceService: WorkspaceService
    @MockBean private lateinit var workspaceInheritanceService: WorkspaceInheritanceService
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeWorkspace(id: Long, name: String = "ws-$id", parentId: Long? = null) =
        Workspace(id = id, name = name, ownerId = 1L, parentWorkspaceId = parentId)

    private fun makeWorkspaceResponse(id: Long, name: String = "ws-$id", parentId: Long? = null) =
        WorkspaceResponse(
            id = id, name = name, description = null, ownerId = 1L,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now(),
            parentWorkspaceId = parentId, inheritanceEnabled = parentId != null
        )

    @Test
    fun `GET children returns list of child workspaces`() {
        val children = listOf(makeWorkspace(2L, parentId = 1L), makeWorkspace(3L, parentId = 1L))
        whenever(workspaceInheritanceService.listChildWorkspaces(1L)).thenReturn(children)

        mockMvc.perform(get("/api/workspaces/1/children"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[1].id").value(3))
    }

    @Test
    fun `GET children returns empty list when no children`() {
        whenever(workspaceInheritanceService.listChildWorkspaces(1L)).thenReturn(emptyList())

        mockMvc.perform(get("/api/workspaces/1/children"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `POST children creates child workspace and returns 201`() {
        val response = makeWorkspaceResponse(id = 5L, name = "child", parentId = 1L)
        whenever(workspaceService.createWorkspace(any<WorkspaceRequest>(), any())).thenReturn(response)
        whenever(userRepository.findByUsername(any())).thenReturn(
            java.util.Optional.of(com.opendatamask.model.User(id = 1L, username = "user", email = "u@t.com", passwordHash = "hash"))
        )

        val body = objectMapper.writeValueAsString(
            CreateChildWorkspaceRequest(name = "child", inheritanceEnabled = true)
        )
        mockMvc.perform(
            post("/api/workspaces/1/children")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .principal { "user" }
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.name").value("child"))
    }

    @Test
    fun `POST inherit triggers inheritFromParent and returns 200`() {
        doNothing().whenever(workspaceInheritanceService).inheritFromParent(2L, 1L)

        mockMvc.perform(post("/api/workspaces/2/inherit/1"))
            .andExpect(status().isOk)

        verify(workspaceInheritanceService).inheritFromParent(2L, 1L)
    }

    @Test
    fun `POST sync-parent triggers syncWithParent and returns 200`() {
        doNothing().whenever(workspaceInheritanceService).syncWithParent(2L)

        mockMvc.perform(post("/api/workspaces/2/sync-parent"))
            .andExpect(status().isOk)

        verify(workspaceInheritanceService).syncWithParent(2L)
    }

    @Test
    fun `GET inherited-configs returns list of inherited configs`() {
        val configs = listOf(
            InheritedConfig(
                id = 1L, childWorkspaceId = 2L, parentWorkspaceId = 1L,
                configType = "TABLE_CONFIG", tableName = "users", columnName = null,
                inheritedEntityId = 10L, inheritedAt = Instant.now()
            ),
            InheritedConfig(
                id = 2L, childWorkspaceId = 2L, parentWorkspaceId = 1L,
                configType = "COLUMN_GENERATOR", tableName = "users", columnName = "email",
                inheritedEntityId = 20L, inheritedAt = Instant.now()
            )
        )
        whenever(workspaceInheritanceService.listInheritedConfigs(2L)).thenReturn(configs)

        mockMvc.perform(get("/api/workspaces/2/inherited-configs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].configType").value("TABLE_CONFIG"))
            .andExpect(jsonPath("$[1].configType").value("COLUMN_GENERATOR"))
            .andExpect(jsonPath("$[1].columnName").value("email"))
    }

    @Test
    fun `POST override marks config as overridden and returns 200`() {
        doNothing().whenever(workspaceInheritanceService).markAsOverridden(5L)

        mockMvc.perform(post("/api/workspaces/2/inherited-configs/5/override"))
            .andExpect(status().isOk)

        verify(workspaceInheritanceService).markAsOverridden(5L)
    }

    @Test
    fun `POST sync-parent returns 409 when workspace has no parent`() {
        whenever(workspaceInheritanceService.syncWithParent(2L))
            .thenThrow(IllegalStateException("Workspace 2 has no parent workspace configured"))

        mockMvc.perform(post("/api/workspaces/2/sync-parent"))
            .andExpect(status().isConflict)
    }
}
