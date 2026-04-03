package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.ConsistencyMode
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.port.input.dto.ColumnGeneratorResponse
import com.opendatamask.domain.port.input.dto.GeneratorPresetResponse
import com.opendatamask.application.service.GeneratorPresetService
import com.opendatamask.infrastructure.security.JwtTokenProvider
import com.opendatamask.infrastructure.security.UserDetailsServiceImpl
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
import java.time.LocalDateTime

@WebMvcTest(
    GeneratorPresetController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class]
)
@ActiveProfiles("test")
class GeneratorPresetControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var generatorPresetService: GeneratorPresetService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makePresetResponse(id: Long = 1L, workspaceId: Long? = 1L) = GeneratorPresetResponse(
        id = id,
        name = "Test Preset",
        generatorType = GeneratorType.EMAIL,
        generatorParams = null,
        workspaceId = workspaceId,
        isSystem = workspaceId == null,
        createdAt = Instant.now()
    )

    private fun makeColumnGeneratorResponse() = ColumnGeneratorResponse(
        id = 1L,
        tableConfigurationId = 1L,
        columnName = "email",
        generatorType = GeneratorType.EMAIL,
        generatorParams = null,
        presetId = 1L,
        consistencyMode = ConsistencyMode.RANDOM,
        linkKey = null,
        createdAt = LocalDateTime.now()
    )

    @Test
    fun `GET system presets returns 200 with list`() {
        whenever(generatorPresetService.listSystemPresets())
            .thenReturn(listOf(makePresetResponse(workspaceId = null)))

        mockMvc.perform(get("/api/generator-presets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Test Preset"))
    }

    @Test
    fun `GET workspace presets returns 200 with list`() {
        whenever(generatorPresetService.listWorkspacePresets(1L))
            .thenReturn(listOf(makePresetResponse(1L), makePresetResponse(2L)))

        mockMvc.perform(get("/api/workspaces/1/generator-presets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `POST create preset returns 201`() {
        whenever(generatorPresetService.createPreset(eq(1L), any()))
            .thenReturn(makePresetResponse())

        mockMvc.perform(
            post("/api/workspaces/1/generator-presets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Preset\",\"generatorType\":\"EMAIL\"}")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Preset"))
    }

    @Test
    fun `PUT update preset returns 200`() {
        whenever(generatorPresetService.updatePreset(eq(1L), eq(1L), any()))
            .thenReturn(makePresetResponse())

        mockMvc.perform(
            put("/api/workspaces/1/generator-presets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Preset\",\"generatorType\":\"EMAIL\"}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    fun `DELETE preset returns 204`() {
        doNothing().whenever(generatorPresetService).deletePreset(1L, 1L)

        mockMvc.perform(delete("/api/workspaces/1/generator-presets/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE preset returns 404 when not found`() {
        whenever(generatorPresetService.deletePreset(1L, 99L))
            .thenThrow(NoSuchElementException("Preset not found: 99"))

        mockMvc.perform(delete("/api/workspaces/1/generator-presets/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST apply preset to column returns 200`() {
        whenever(generatorPresetService.applyPresetToColumn(1L, "users", "email", 1L))
            .thenReturn(makeColumnGeneratorResponse())

        mockMvc.perform(
            post("/api/workspaces/1/tables/users/columns/email/generator/preset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"presetId\":1}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.columnName").value("email"))
            .andExpect(jsonPath("$.generatorType").value("EMAIL"))
    }
}
