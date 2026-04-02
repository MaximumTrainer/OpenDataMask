package com.opendatamask.adapter.input.rest

import com.opendatamask.adapter.input.rest.dto.WorkspaceResponse
import com.opendatamask.adapter.input.rest.dto.WorkspaceUserResponse
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.adapter.output.persistence.WorkspaceTagRepository
import com.opendatamask.infrastructure.security.JwtTokenProvider
import com.opendatamask.infrastructure.security.UserDetailsServiceImpl
import com.opendatamask.application.service.WorkspaceService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(
    WorkspaceController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class]
)
@ActiveProfiles("test")
class WorkspaceControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var workspaceService: WorkspaceService
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var tagRepository: WorkspaceTagRepository
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeWorkspaceResponse(id: Long = 1L, name: String = "My Workspace") = WorkspaceResponse(
        id = id, name = name, description = null, ownerId = 1L,
        createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
    )

    @Test
    fun `GET workspace by id returns 200`() {
        whenever(workspaceService.getWorkspace(1L)).thenReturn(makeWorkspaceResponse(id = 1L))

        mockMvc.perform(get("/api/workspaces/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("My Workspace"))
    }

    @Test
    fun `GET workspace by id returns 404 when not found`() {
        whenever(workspaceService.getWorkspace(99L)).thenThrow(NoSuchElementException("Workspace not found: 99"))

        mockMvc.perform(get("/api/workspaces/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET users in workspace returns 200`() {
        val users = listOf(
            WorkspaceUserResponse(
                id = 1L, workspaceId = 1L, userId = 1L,
                username = "alice", email = "alice@test.com", role = "ADMIN"
            )
        )
        whenever(workspaceService.getUsersInWorkspace(1L)).thenReturn(users)

        mockMvc.perform(get("/api/workspaces/1/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].username").value("alice"))
    }

    @Test
    fun `GET users in workspace returns empty list`() {
        whenever(workspaceService.getUsersInWorkspace(1L)).thenReturn(emptyList())

        mockMvc.perform(get("/api/workspaces/1/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }
}
