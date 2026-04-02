package com.opendatamask.adapter.input.rest

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opendatamask.domain.port.input.dto.DataConnectionRequest
import com.opendatamask.domain.port.input.dto.DataConnectionResponse
import com.opendatamask.domain.port.input.dto.ConnectionTestResult
import com.opendatamask.domain.model.ConnectionType
import com.opendatamask.infrastructure.security.JwtAuthenticationFilter
import com.opendatamask.infrastructure.security.JwtTokenProvider
import com.opendatamask.infrastructure.security.UserDetailsServiceImpl
import com.opendatamask.application.service.DataConnectionService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(
    DataConnectionController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class]
)
@ActiveProfiles("test")
class DataConnectionControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var dataConnectionService: DataConnectionService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private fun makeResponse(id: Long = 1L, workspaceId: Long = 1L) = DataConnectionResponse(
        id = id, workspaceId = workspaceId, name = "My DB", type = ConnectionType.POSTGRESQL,
        username = "user", database = null, isSource = true, isDestination = false,
        createdAt = LocalDateTime.now()
    )

    @Test
    fun `POST create connection returns 201`() {
        val request = DataConnectionRequest(
            name = "My DB", type = ConnectionType.POSTGRESQL,
            connectionString = "jdbc:postgresql://localhost/db",
            isSource = true, isDestination = false
        )
        whenever(dataConnectionService.createConnection(eq(1L), any())).thenReturn(makeResponse())

        mockMvc.perform(
            post("/api/workspaces/1/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
    }

    @Test
    fun `GET connection by id returns 200`() {
        whenever(dataConnectionService.getConnection(1L, 1L)).thenReturn(makeResponse())

        mockMvc.perform(get("/api/workspaces/1/connections/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("My DB"))
    }

    @Test
    fun `GET list connections returns 200`() {
        whenever(dataConnectionService.listConnections(1L)).thenReturn(listOf(makeResponse(id = 1L), makeResponse(id = 2L)))

        mockMvc.perform(get("/api/workspaces/1/connections"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `PUT update connection returns 200`() {
        val request = DataConnectionRequest(
            name = "Updated DB", type = ConnectionType.POSTGRESQL,
            connectionString = "jdbc:postgresql://localhost/updated",
            isSource = true, isDestination = false
        )
        whenever(dataConnectionService.updateConnection(eq(1L), eq(1L), any())).thenReturn(makeResponse())

        mockMvc.perform(
            put("/api/workspaces/1/connections/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        ).andExpect(status().isOk)
    }

    @Test
    fun `DELETE connection returns 204`() {
        mockMvc.perform(delete("/api/workspaces/1/connections/1"))
            .andExpect(status().isNoContent)

        verify(dataConnectionService).deleteConnection(1L, 1L)
    }

    @Test
    fun `POST test connection returns 200 with success result`() {
        whenever(dataConnectionService.testConnection(1L, 1L))
            .thenReturn(ConnectionTestResult(success = true, message = "Connection successful"))

        mockMvc.perform(post("/api/workspaces/1/connections/1/test"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Connection successful"))
    }

    @Test
    fun `POST create connection returns 400 for missing name`() {
        val request = mapOf("type" to "POSTGRESQL", "connectionString" to "jdbc:...")

        mockMvc.perform(
            post("/api/workspaces/1/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }
}

