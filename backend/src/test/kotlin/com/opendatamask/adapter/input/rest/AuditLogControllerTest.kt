package com.opendatamask.adapter.input.rest

import com.opendatamask.application.service.AuditService
import com.opendatamask.domain.model.AuditAction
import com.opendatamask.domain.model.AuditResourceType
import com.opendatamask.domain.port.input.dto.AuditLogResponse
import com.opendatamask.infrastructure.security.JwtTokenProvider
import com.opendatamask.infrastructure.security.UserDetailsServiceImpl
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@WebMvcTest(
    AuditLogController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(AuditLogControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class AuditLogControllerTest {

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

    @MockBean private lateinit var auditService: AuditService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeEntry(id: Long = 1L, workspaceId: Long = 1L) = AuditLogResponse(
        id = id,
        timestamp = Instant.parse("2024-01-01T00:00:00Z"),
        actorId = 42L,
        actorUsername = "alice",
        action = AuditAction.JOB_STARTED,
        resourceType = AuditResourceType.JOB,
        resourceId = "99",
        workspaceId = workspaceId,
        beforeJson = null,
        afterJson = """{"status":"RUNNING"}""",
        ipAddress = "127.0.0.1",
        description = "Job started by alice"
    )

    @Test
    fun `GET workspace audit log returns 200 with entries`() {
        whenever(auditService.getWorkspaceAuditLog(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(makeEntry(1L), makeEntry(2L)))

        mockMvc.perform(get("/api/workspaces/1/audit-log"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].action").value("JOB_STARTED"))
            .andExpect(jsonPath("$[0].actorUsername").value("alice"))
    }

    @Test
    fun `GET workspace audit log returns 200 with empty list when no entries`() {
        whenever(auditService.getWorkspaceAuditLog(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(emptyList())

        mockMvc.perform(get("/api/workspaces/1/audit-log"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `GET global audit log returns 200 with entries`() {
        whenever(auditService.getGlobalAuditLog(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(makeEntry(1L, 1L), makeEntry(2L, 2L)))

        mockMvc.perform(get("/api/audit-log"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[1].workspaceId").value(2))
    }

    @Test
    fun `GET workspace audit log passes date range params`() {
        whenever(auditService.getWorkspaceAuditLog(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(makeEntry()))

        mockMvc.perform(
            get("/api/workspaces/1/audit-log")
                .param("from", "2024-01-01T00:00:00Z")
                .param("to", "2024-12-31T23:59:59Z")
                .param("limit", "100")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET global audit log returns resource type and description`() {
        whenever(auditService.getGlobalAuditLog(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(makeEntry()))

        mockMvc.perform(get("/api/audit-log"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].resourceType").value("JOB"))
            .andExpect(jsonPath("$[0].resourceId").value("99"))
            .andExpect(jsonPath("$[0].description").value("Job started by alice"))
    }
}
