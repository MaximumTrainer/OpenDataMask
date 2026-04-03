package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.port.input.dto.WorkspaceConfigDto
import com.opendatamask.application.service.WorkspaceExportService
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
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(
    WorkspaceExportController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(WorkspaceExportControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class WorkspaceExportControllerTest {

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

    @MockBean private lateinit var exportService: WorkspaceExportService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    @Test
    fun `GET export returns 200`() {
        whenever(exportService.export(1L)).thenReturn(WorkspaceConfigDto(version = "1.0"))

        mockMvc.perform(get("/api/workspaces/1/export"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET export has Content-Disposition header with attachment`() {
        whenever(exportService.export(1L)).thenReturn(WorkspaceConfigDto(version = "1.0"))

        mockMvc.perform(get("/api/workspaces/1/export"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment")))
    }

    @Test
    fun `GET export has Content-Type application json`() {
        whenever(exportService.export(1L)).thenReturn(WorkspaceConfigDto(version = "1.0"))

        mockMvc.perform(get("/api/workspaces/1/export"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `POST import returns 200 with status imported`() {
        doNothing().whenever(exportService).import(eq(1L), any())

        mockMvc.perform(
            post("/api/workspaces/1/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"version":"1.0","tables":[],"actions":[]}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("imported"))
    }

    @Test
    fun `POST import response body contains version field`() {
        doNothing().whenever(exportService).import(eq(1L), any())

        mockMvc.perform(
            post("/api/workspaces/1/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"version":"1.0","tables":[],"actions":[]}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value("1.0"))
    }

    @Test
    fun `GET export when workspace not found returns 404`() {
        whenever(exportService.export(99L))
            .thenThrow(NoSuchElementException("Workspace not found: 99"))

        mockMvc.perform(get("/api/workspaces/99/export"))
            .andExpect(status().isNotFound)
    }
}
