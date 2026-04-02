package com.opendatamask.controller

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opendatamask.dto.ColumnGeneratorRequest
import com.opendatamask.dto.ColumnGeneratorResponse
import com.opendatamask.dto.TableConfigurationRequest
import com.opendatamask.dto.TableConfigurationResponse
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.model.TableMode
import com.opendatamask.domain.model.User
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.security.JwtAuthenticationFilter
import com.opendatamask.security.JwtTokenProvider
import com.opendatamask.security.UserDetailsServiceImpl
import com.opendatamask.service.PermissionService
import com.opendatamask.service.TableConfigurationService
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
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(
    TableConfigurationController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(TableConfigurationControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class TableConfigurationControllerTest {

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

    @MockBean private lateinit var tableConfigurationService: TableConfigurationService
    @MockBean private lateinit var permissionService: PermissionService
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val mockUser = User(id = 1L, username = "testuser", email = "test@example.com", passwordHash = "hash")

    private fun setupAuth() {
        whenever(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser))
    }

    private fun makeConfigResponse(id: Long = 1L, workspaceId: Long = 1L) = TableConfigurationResponse(
        id = id, workspaceId = workspaceId, tableName = "users", schemaName = null,
        mode = TableMode.PASSTHROUGH, rowLimit = null, whereClause = null,
        createdAt = LocalDateTime.now()
    )

    private fun makeGeneratorResponse(id: Long = 1L, tableConfigId: Long = 1L) = ColumnGeneratorResponse(
        id = id, tableConfigurationId = tableConfigId, columnName = "email",
        generatorType = GeneratorType.EMAIL, generatorParams = null,
        presetId = null, consistencyMode = com.opendatamask.domain.model.ConsistencyMode.RANDOM, linkKey = null,
        createdAt = LocalDateTime.now()
    )

    @Test
    @WithMockUser(username = "testuser")
    fun `POST create table config returns 201`() {
        setupAuth()
        val request = TableConfigurationRequest(tableName = "users", mode = TableMode.PASSTHROUGH)
        whenever(tableConfigurationService.createTableConfiguration(eq(1L), any())).thenReturn(makeConfigResponse())

        mockMvc.perform(
            post("/api/workspaces/1/tables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    fun `GET table config by id returns 200`() {
        whenever(tableConfigurationService.getTableConfiguration(1L, 1L)).thenReturn(makeConfigResponse())

        mockMvc.perform(get("/api/workspaces/1/tables/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tableName").value("users"))
    }

    @Test
    fun `GET list table configs returns 200`() {
        whenever(tableConfigurationService.listTableConfigurations(1L))
            .thenReturn(listOf(makeConfigResponse(id = 1L), makeConfigResponse(id = 2L)))

        mockMvc.perform(get("/api/workspaces/1/tables"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `PUT update table config returns 200`() {
        setupAuth()
        val request = TableConfigurationRequest(tableName = "orders", mode = TableMode.MASK)
        whenever(tableConfigurationService.updateTableConfiguration(eq(1L), eq(1L), any())).thenReturn(makeConfigResponse())

        mockMvc.perform(
            put("/api/workspaces/1/tables/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        ).andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `DELETE table config returns 204`() {
        setupAuth()
        mockMvc.perform(delete("/api/workspaces/1/tables/1"))
            .andExpect(status().isNoContent)

        verify(tableConfigurationService).deleteTableConfiguration(1L, 1L)
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `POST add column generator returns 201`() {
        setupAuth()
        val request = ColumnGeneratorRequest(columnName = "email", generatorType = GeneratorType.EMAIL)
        whenever(tableConfigurationService.addColumnGenerator(eq(1L), any())).thenReturn(makeGeneratorResponse())

        mockMvc.perform(
            post("/api/workspaces/1/tables/1/generators")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.columnName").value("email"))
    }

    @Test
    fun `GET list column generators returns 200`() {
        whenever(tableConfigurationService.listColumnGenerators(1L))
            .thenReturn(listOf(makeGeneratorResponse(id = 1L), makeGeneratorResponse(id = 2L)))

        mockMvc.perform(get("/api/workspaces/1/tables/1/generators"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `PUT update column generator returns 200`() {
        setupAuth()
        val request = ColumnGeneratorRequest(columnName = "phone", generatorType = GeneratorType.PHONE)
        whenever(tableConfigurationService.updateColumnGenerator(eq(1L), eq(1L), any())).thenReturn(makeGeneratorResponse())

        mockMvc.perform(
            put("/api/workspaces/1/tables/1/generators/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        ).andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `DELETE column generator returns 204`() {
        setupAuth()
        mockMvc.perform(delete("/api/workspaces/1/tables/1/generators/1"))
            .andExpect(status().isNoContent)

        verify(tableConfigurationService).deleteColumnGenerator(1L, 1L)
    }

    @Test
    fun `POST create table config returns 400 for missing tableName`() {
        val request = mapOf("mode" to "PASSTHROUGH")

        mockMvc.perform(
            post("/api/workspaces/1/tables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }
}
