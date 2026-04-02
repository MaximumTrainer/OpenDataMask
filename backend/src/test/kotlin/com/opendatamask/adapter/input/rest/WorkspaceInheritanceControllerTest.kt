package com.opendatamask.adapter.input.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.opendatamask.adapter.input.rest.dto.*
import com.opendatamask.domain.port.input.dto.*
import com.opendatamask.domain.model.InheritedConfig
import com.opendatamask.domain.model.Workspace
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.infrastructure.security.JwtTokenProvider
import com.opendatamask.infrastructure.security.UserDetailsServiceImpl
import com.opendatamask.application.service.WorkspaceInheritanceService
import com.opendatamask.application.service.WorkspaceService
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
    fun `GET children returns 404 when parent workspace not found`() {
        whenever(workspaceInheritanceService.listChildWorkspaces(99L))
            .thenThrow(NoSuchElementException("Workspace not found: 99"))

        mockMvc.perform(get("/api/workspaces/99/children"))
            .andExpect(status().isNotFound)
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
    fun `POST override returns 404 when inherited config not found`() {
        whenever(workspaceInheritanceService.markAsOverridden(99L))
            .thenThrow(NoSuchElementException("InheritedConfig not found: 99"))

        mockMvc.perform(post("/api/workspaces/2/inherited-configs/99/override"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST sync-parent returns 409 when workspace has no parent`() {
        whenever(workspaceInheritanceService.syncWithParent(2L))
            .thenThrow(IllegalStateException("Workspace 2 has no parent workspace configured"))

        mockMvc.perform(post("/api/workspaces/2/sync-parent"))
            .andExpect(status().isConflict)
    }
}

