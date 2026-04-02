package com.opendatamask.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opendatamask.domain.model.User
import com.opendatamask.domain.model.WorkspacePermission
import com.opendatamask.domain.model.WorkspaceRole
import com.opendatamask.domain.model.WorkspaceUser
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.adapter.output.persistence.WorkspaceUserPermissionRepository
import com.opendatamask.adapter.output.persistence.WorkspaceUserRepository
import com.opendatamask.security.JwtTokenProvider
import com.opendatamask.security.UserDetailsServiceImpl
import com.opendatamask.service.PermissionService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.Optional

@WebMvcTest(
    WorkspacePermissionController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(WorkspacePermissionControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class WorkspacePermissionControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    class TestSecurityConfig {
        @Bean
        fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http.csrf { it.disable() }
                .authorizeHttpRequests { it.anyRequest().permitAll() }
            return http.build()
        }
    }

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var permissionService: PermissionService
    @MockBean private lateinit var workspaceUserRepository: WorkspaceUserRepository
    @MockBean private lateinit var workspaceUserPermissionRepository: WorkspaceUserPermissionRepository
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private val mapper = jacksonObjectMapper()

    private val adminUser = User(id = 1L, username = "admin", email = "admin@example.com", passwordHash = "hash")
    private val adminWorkspaceUser = WorkspaceUser(id = 1L, workspaceId = 1L, userId = 1L, role = WorkspaceRole.ADMIN)
    private val targetWorkspaceUser = WorkspaceUser(id = 2L, workspaceId = 1L, userId = 2L, role = WorkspaceRole.USER)

    private fun setupAdminAuth() {
        whenever(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser))
        whenever(workspaceUserRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .thenReturn(Optional.of(adminWorkspaceUser))
    }

    @Test
    @WithMockUser(username = "admin")
    fun `GET returns effective permissions`() {
        setupAdminAuth()
        whenever(permissionService.getEffectivePermissions(2L, 1L))
            .thenReturn(setOf(WorkspacePermission.CONFIGURE_GENERATORS, WorkspacePermission.RUN_JOBS))

        mockMvc.perform(get("/api/workspaces/1/users/2/permissions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @WithMockUser(username = "admin")
    fun `PUT updates permissions and returns new effective set`() {
        setupAdminAuth()
        whenever(workspaceUserRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .thenReturn(Optional.of(targetWorkspaceUser))
        whenever(workspaceUserPermissionRepository.findByWorkspaceUserId(2L)).thenReturn(emptyList())
        whenever(workspaceUserPermissionRepository.save(any<com.opendatamask.domain.model.WorkspaceUserPermission>())).thenAnswer { it.arguments[0] }
        whenever(permissionService.getEffectivePermissions(2L, 1L))
            .thenReturn(
                setOf(
                    WorkspacePermission.CONFIGURE_GENERATORS,
                    WorkspacePermission.CONFIGURE_SENSITIVITY,
                    WorkspacePermission.RUN_JOBS
                )
            )

        val request = mapOf("grants" to listOf("CONFIGURE_SENSITIVITY"), "revocations" to emptyList<String>())

        mockMvc.perform(
            put("/api/workspaces/1/users/2/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    @WithMockUser(username = "admin")
    fun `DELETE clears all overrides and returns 204`() {
        setupAdminAuth()
        whenever(workspaceUserRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .thenReturn(Optional.of(targetWorkspaceUser))
        doNothing().whenever(workspaceUserPermissionRepository).deleteByWorkspaceUserId(2L)

        mockMvc.perform(delete("/api/workspaces/1/users/2/permissions"))
            .andExpect(status().isNoContent)

        verify(workspaceUserPermissionRepository).deleteByWorkspaceUserId(2L)
    }
}
