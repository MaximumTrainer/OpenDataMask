package com.opendatamask.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opendatamask.dto.ForeignKeyRelationshipRequest
import com.opendatamask.dto.ForeignKeyRelationshipResponse
import com.opendatamask.domain.model.ForeignKeyRelationship
import com.opendatamask.security.JwtAuthenticationFilter
import com.opendatamask.security.JwtTokenProvider
import com.opendatamask.security.UserDetailsServiceImpl
import com.opendatamask.service.ForeignKeyDiscoveryService
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
import java.time.Instant

@WebMvcTest(
    ForeignKeyRelationshipController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class]
)
@ActiveProfiles("test")
class ForeignKeyRelationshipControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var foreignKeyDiscoveryService: ForeignKeyDiscoveryService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private val mapper = jacksonObjectMapper()

    private fun makeFkResponse(id: Long = 1L) = ForeignKeyRelationshipResponse(
        id = id,
        workspaceId = 1L,
        fromTable = "orders",
        fromColumn = "customer_id",
        toTable = "customers",
        toColumn = "id",
        isVirtual = true,
        discoveredAt = Instant.now()
    )

    private fun makeFk(id: Long = 1L) = ForeignKeyRelationship(
        id = id, workspaceId = 1L, fromTable = "orders", fromColumn = "customer_id",
        toTable = "customers", toColumn = "id", isVirtual = true
    )

    @Test
    fun `GET foreign-keys returns 200 with list`() {
        whenever(foreignKeyDiscoveryService.getVirtualForeignKeys(1L)).thenReturn(listOf(makeFk()))

        mockMvc.perform(get("/api/workspaces/1/foreign-keys"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].fromTable").value("orders"))
    }

    @Test
    fun `POST foreign-keys creates virtual FK and returns 201`() {
        val request = ForeignKeyRelationshipRequest(
            fromTable = "orders", fromColumn = "customer_id",
            toTable = "customers", toColumn = "id"
        )
        whenever(
            foreignKeyDiscoveryService.createVirtualForeignKey(1L, "orders", "customer_id", "customers", "id")
        ).thenReturn(makeFk())

        mockMvc.perform(
            post("/api/workspaces/1/foreign-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.fromTable").value("orders"))
    }

    @Test
    fun `DELETE foreign-keys returns 204`() {
        mockMvc.perform(delete("/api/workspaces/1/foreign-keys/1"))
            .andExpect(status().isNoContent)

        verify(foreignKeyDiscoveryService).deleteForeignKey(1L, 1L)
    }

    @Test
    fun `POST discover triggers discovery and returns list`() {
        whenever(foreignKeyDiscoveryService.discoverForeignKeys(1L)).thenReturn(listOf(makeFk()))

        mockMvc.perform(post("/api/workspaces/1/foreign-keys/discover"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `POST foreign-keys returns 400 when fromTable is blank`() {
        val badRequest = mapOf("fromTable" to "", "fromColumn" to "customer_id", "toTable" to "customers", "toColumn" to "id")

        mockMvc.perform(
            post("/api/workspaces/1/foreign-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(badRequest))
        ).andExpect(status().isBadRequest)
    }
}
