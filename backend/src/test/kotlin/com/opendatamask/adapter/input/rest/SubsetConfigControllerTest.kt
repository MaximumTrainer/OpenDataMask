package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.SubsetLimitType
import com.opendatamask.domain.model.SubsetTableConfig
import com.opendatamask.application.service.SubsetConfigService
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

@WebMvcTest(
    SubsetConfigController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(SubsetConfigControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class SubsetConfigControllerTest {

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

    @MockBean private lateinit var subsetConfigService: SubsetConfigService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeConfig(id: Long = 1L) = SubsetTableConfig(
        id = id,
        workspaceId = 1L,
        tableName = "users",
        limitType = SubsetLimitType.PERCENTAGE,
        limitValue = 10,
        isTargetTable = false,
        isLookupTable = false
    )

    private val validRequestBody = """{"tableName":"users","limitType":"PERCENTAGE","limitValue":10,"isTargetTable":false,"isLookupTable":false}"""

    @Test
    fun `GET list subset configs returns 200 with list`() {
        whenever(subsetConfigService.listConfigs(1L)).thenReturn(listOf(makeConfig(1L), makeConfig(2L)))

        mockMvc.perform(get("/api/workspaces/1/subset-config"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].tableName").value("users"))
    }

    @Test
    fun `POST create subset config returns 201`() {
        whenever(subsetConfigService.createOrUpdateConfig(eq(1L), any())).thenReturn(makeConfig())

        mockMvc.perform(
            post("/api/workspaces/1/subset-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.tableName").value("users"))
            .andExpect(jsonPath("$.limitType").value("PERCENTAGE"))
    }

    @Test
    fun `PUT update subset config returns 200`() {
        whenever(subsetConfigService.updateConfig(eq(1L), eq(1L), any())).thenReturn(makeConfig())

        mockMvc.perform(
            put("/api/workspaces/1/subset-config/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    fun `DELETE subset config returns 204`() {
        doNothing().whenever(subsetConfigService).deleteConfig(1L, 1L)

        mockMvc.perform(delete("/api/workspaces/1/subset-config/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `GET returns 404 when workspace not found`() {
        whenever(subsetConfigService.listConfigs(99L))
            .thenThrow(NoSuchElementException("Workspace not found: 99"))

        mockMvc.perform(get("/api/workspaces/99/subset-config"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT returns 404 when config not found`() {
        whenever(subsetConfigService.updateConfig(eq(1L), eq(99L), any()))
            .thenThrow(NoSuchElementException("Config not found: 99"))

        mockMvc.perform(
            put("/api/workspaces/1/subset-config/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestBody)
        )
            .andExpect(status().isNotFound)
    }
}
