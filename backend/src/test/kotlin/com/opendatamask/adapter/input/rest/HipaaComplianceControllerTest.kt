package com.opendatamask.adapter.input.rest

import com.opendatamask.application.service.HipaaComplianceService
import com.opendatamask.domain.port.input.dto.*
import com.opendatamask.infrastructure.security.JwtTokenProvider
import com.opendatamask.infrastructure.security.UserDetailsServiceImpl
import org.junit.jupiter.api.Test
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

@WebMvcTest(
    HipaaComplianceController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(HipaaComplianceControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class HipaaComplianceControllerTest {

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

    @MockBean private lateinit var hipaaComplianceService: HipaaComplianceService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeReport(workspaceId: Long = 1L) = HipaaComplianceReport(
        workspaceId = workspaceId,
        overallStatus = HipaaComplianceStatus.COMPLIANT,
        compliantCategories = 15,
        nonCompliantCategories = 0,
        notDetectedCategories = 0,
        categories = listOf(
            HipaaPhiCategory(
                categoryId = "names",
                displayName = "Names",
                description = "Full or partial names",
                status = HipaaComplianceStatus.COMPLIANT,
                affectedColumns = listOf(
                    HipaaPhiColumnDetail(
                        tableName = "users",
                        columnName = "full_name",
                        sensitivityType = "PHI_NAME",
                        isMasked = true,
                        appliedGenerator = "NAME"
                    )
                )
            )
        )
    )

    @Test
    fun `GET hipaa-status returns 200 with compliance report`() {
        whenever(hipaaComplianceService.getComplianceReport(1L)).thenReturn(makeReport())

        mockMvc.perform(get("/api/workspaces/1/hipaa-status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.workspaceId").value(1))
            .andExpect(jsonPath("$.overallStatus").value("COMPLIANT"))
            .andExpect(jsonPath("$.compliantCategories").value(15))
            .andExpect(jsonPath("$.nonCompliantCategories").value(0))
            .andExpect(jsonPath("$.categories.length()").value(1))
            .andExpect(jsonPath("$.categories[0].categoryId").value("names"))
    }

    @Test
    fun `GET hipaa-status returns report with non-compliant categories`() {
        val report = HipaaComplianceReport(
            workspaceId = 2L,
            overallStatus = HipaaComplianceStatus.NON_COMPLIANT,
            compliantCategories = 10,
            nonCompliantCategories = 5,
            notDetectedCategories = 0,
            categories = emptyList()
        )
        whenever(hipaaComplianceService.getComplianceReport(2L)).thenReturn(report)

        mockMvc.perform(get("/api/workspaces/2/hipaa-status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.overallStatus").value("NON_COMPLIANT"))
            .andExpect(jsonPath("$.nonCompliantCategories").value(5))
    }

    @Test
    fun `GET hipaa-status returns report with column masking details`() {
        whenever(hipaaComplianceService.getComplianceReport(1L)).thenReturn(makeReport())

        mockMvc.perform(get("/api/workspaces/1/hipaa-status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.categories[0].affectedColumns[0].tableName").value("users"))
            .andExpect(jsonPath("$.categories[0].affectedColumns[0].isMasked").value(true))
            .andExpect(jsonPath("$.categories[0].affectedColumns[0].appliedGenerator").value("NAME"))
    }
}
