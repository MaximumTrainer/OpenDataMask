package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.PrivacyReport
import com.opendatamask.application.service.PrivacyReportService
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(
    PrivacyReportController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(PrivacyReportControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class PrivacyReportControllerTest {

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

    @MockBean private lateinit var privacyReportService: PrivacyReportService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeReport(id: Long = 1L, workspaceId: Long = 1L) = PrivacyReport(
        id = id,
        workspaceId = workspaceId,
        reportType = "CURRENT_CONFIG",
        reportJson = """{"workspaceId":1}"""
    )

    @Test
    fun `GET privacy report returns 200 when existing report found`() {
        val report = makeReport()
        whenever(privacyReportService.getLatestCurrentReport(any(), any())).thenReturn(report)

        mockMvc.perform(get("/api/workspaces/1/privacy-report"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.reportType").value("CURRENT_CONFIG"))
    }

    @Test
    fun `GET privacy report generates new report when no existing report`() {
        val generated = makeReport(id = 2L)
        whenever(privacyReportService.getLatestCurrentReport(any(), any())).thenReturn(null)
        whenever(privacyReportService.generateCurrentConfigReport(1L)).thenReturn(generated)

        mockMvc.perform(get("/api/workspaces/1/privacy-report"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(2))

        verify(privacyReportService).generateCurrentConfigReport(1L)
    }

    @Test
    fun `GET privacy report download returns 200 with Content-Disposition attachment header`() {
        val report = makeReport()
        whenever(privacyReportService.getLatestCurrentReport(any(), any())).thenReturn(report)

        mockMvc.perform(get("/api/workspaces/1/privacy-report/download"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment")))
    }

    @Test
    fun `GET privacy report download has correct filename format`() {
        val report = makeReport(id = 42L)
        whenever(privacyReportService.getLatestCurrentReport(any(), any())).thenReturn(report)

        mockMvc.perform(get("/api/workspaces/1/privacy-report/download"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("privacy-report-42")))
    }

    @Test
    fun `GET job privacy report returns 200 when existing job report found`() {
        val report = PrivacyReport(
            id = 10L,
            workspaceId = 1L,
            jobId = 5L,
            reportType = "JOB",
            reportJson = """{"jobId":5}"""
        )
        whenever(privacyReportService.getJobReport(5L)).thenReturn(report)

        mockMvc.perform(get("/api/workspaces/1/jobs/5/privacy-report"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.jobId").value(5))
            .andExpect(jsonPath("$.reportType").value("JOB"))
    }

    @Test
    fun `GET job privacy report generates report when no existing job report`() {
        val generated = PrivacyReport(
            id = 11L,
            workspaceId = 1L,
            jobId = 5L,
            reportType = "JOB",
            reportJson = """{"jobId":5}"""
        )
        whenever(privacyReportService.getJobReport(5L)).thenReturn(null)
        whenever(privacyReportService.generateJobReport(5L, 1L)).thenReturn(generated)

        mockMvc.perform(get("/api/workspaces/1/jobs/5/privacy-report"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(11))

        verify(privacyReportService).generateJobReport(5L, 1L)
    }
}
