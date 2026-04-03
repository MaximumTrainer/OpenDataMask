package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.SchemaChange
import com.opendatamask.domain.model.SchemaChangeStatus
import com.opendatamask.domain.model.SchemaChangeType
import com.opendatamask.domain.model.Workspace
import com.opendatamask.adapter.output.persistence.WorkspaceRepository
import com.opendatamask.application.service.SchemaChangeService
import com.opendatamask.infrastructure.security.JwtTokenProvider
import com.opendatamask.infrastructure.security.UserDetailsServiceImpl
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
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.Optional

@WebMvcTest(
    SchemaChangeController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(SchemaChangeControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class SchemaChangeControllerTest {

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

    @MockBean private lateinit var schemaChangeService: SchemaChangeService
    @MockBean private lateinit var workspaceRepository: WorkspaceRepository
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeChange(
        id: Long = 1L,
        type: SchemaChangeType = SchemaChangeType.NEW_COLUMN
    ) = SchemaChange(
        id = id,
        workspaceId = 1L,
        changeType = type,
        tableName = "users",
        columnName = "email",
        status = SchemaChangeStatus.UNRESOLVED
    )

    @Test
    fun `GET schema changes returns 200 with exposing and notifications split`() {
        val exposing = makeChange(1L, SchemaChangeType.NEW_COLUMN)
        val notification = makeChange(2L, SchemaChangeType.NEW_TABLE)
        whenever(schemaChangeService.getUnresolvedChanges(1L)).thenReturn(listOf(exposing, notification))

        mockMvc.perform(get("/api/workspaces/1/schema-changes"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.exposing.length()").value(1))
            .andExpect(jsonPath("$.notifications.length()").value(1))
    }

    @Test
    fun `POST detect schema changes returns 200 with list of changes`() {
        val changes = listOf(makeChange(1L), makeChange(2L, SchemaChangeType.DROPPED_COLUMN))
        whenever(schemaChangeService.detectChanges(1L)).thenReturn(changes)

        mockMvc.perform(post("/api/workspaces/1/schema-changes/detect"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `POST resolve change returns 200`() {
        doNothing().whenever(schemaChangeService).resolveChange(1L)

        mockMvc.perform(post("/api/workspaces/1/schema-changes/1/resolve"))
            .andExpect(status().isOk)

        verify(schemaChangeService).resolveChange(1L)
    }

    @Test
    fun `POST dismiss change returns 200`() {
        doNothing().whenever(schemaChangeService).dismissChange(1L)

        mockMvc.perform(post("/api/workspaces/1/schema-changes/1/dismiss"))
            .andExpect(status().isOk)

        verify(schemaChangeService).dismissChange(1L)
    }

    @Test
    fun `POST resolve-all returns 200`() {
        doNothing().whenever(schemaChangeService).resolveAll(1L)

        mockMvc.perform(post("/api/workspaces/1/schema-changes/resolve-all"))
            .andExpect(status().isOk)

        verify(schemaChangeService).resolveAll(1L)
    }

    @Test
    fun `PATCH settings returns 200 and updates schemaChangeHandling`() {
        val workspace = Workspace(id = 1L, name = "Test", ownerId = 1L)
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace))
        whenever(workspaceRepository.save(any())).thenReturn(workspace)

        mockMvc.perform(
            patch("/api/workspaces/1/schema-changes/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"schemaChangeHandling":"NEVER_BLOCK"}""")
        )
            .andExpect(status().isOk)

        verify(workspaceRepository).save(workspace)
    }
}
