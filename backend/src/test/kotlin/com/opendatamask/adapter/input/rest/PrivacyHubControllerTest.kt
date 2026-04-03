package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.port.input.dto.PrivacyHubSummary
import com.opendatamask.domain.port.input.dto.PrivacyRecommendation
import com.opendatamask.application.service.PrivacyHubService
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(
    PrivacyHubController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(PrivacyHubControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class PrivacyHubControllerTest {

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

    @MockBean private lateinit var privacyHubService: PrivacyHubService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeSummary() = PrivacyHubSummary(
        atRiskCount = 2,
        protectedCount = 5,
        notSensitiveCount = 10,
        tables = emptyList(),
        recommendationsCount = 2
    )

    private fun makeRecommendation() = PrivacyRecommendation(
        tableName = "users",
        columnName = "email",
        sensitivityType = "EMAIL",
        confidenceLevel = "HIGH",
        recommendedGenerator = "EMAIL_GENERATOR"
    )

    @Test
    fun `GET privacy hub returns 200 with summary`() {
        whenever(privacyHubService.getSummary(1L)).thenReturn(makeSummary())

        mockMvc.perform(get("/api/workspaces/1/privacy-hub"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.atRiskCount").value(2))
            .andExpect(jsonPath("$.protectedCount").value(5))
    }

    @Test
    fun `GET recommendations returns 200 with list`() {
        whenever(privacyHubService.getRecommendations(1L))
            .thenReturn(listOf(makeRecommendation()))

        mockMvc.perform(get("/api/workspaces/1/privacy-hub/recommendations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].tableName").value("users"))
    }

    @Test
    fun `POST apply recommendations returns 200 with applied count`() {
        whenever(privacyHubService.applyRecommendations(1L)).thenReturn(3)

        mockMvc.perform(post("/api/workspaces/1/privacy-hub/recommendations/apply"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applied").value(3))
    }

    @Test
    fun `GET privacy hub returns 404 for unknown workspace`() {
        whenever(privacyHubService.getSummary(99L))
            .thenThrow(NoSuchElementException("Workspace not found: 99"))

        mockMvc.perform(get("/api/workspaces/99/privacy-hub"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET recommendations returns 200 with empty list`() {
        whenever(privacyHubService.getRecommendations(1L)).thenReturn(emptyList())

        mockMvc.perform(get("/api/workspaces/1/privacy-hub/recommendations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `POST apply recommendations returns 200 with count zero when no recommendations`() {
        whenever(privacyHubService.applyRecommendations(1L)).thenReturn(0)

        mockMvc.perform(post("/api/workspaces/1/privacy-hub/recommendations/apply"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applied").value(0))
    }
}
