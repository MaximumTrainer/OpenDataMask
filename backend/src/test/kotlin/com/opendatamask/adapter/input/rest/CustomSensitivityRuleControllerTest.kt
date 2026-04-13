package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.GenericDataType
import com.opendatamask.domain.model.MatcherType
import com.opendatamask.domain.port.input.dto.CustomRuleMatcherDto
import com.opendatamask.domain.port.input.dto.CustomRulePreviewResult
import com.opendatamask.domain.port.input.dto.CustomSensitivityRuleResponse
import com.opendatamask.application.service.CustomSensitivityRuleService
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

@WebMvcTest(
    CustomSensitivityRuleController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class]
)
@ActiveProfiles("test")
class CustomSensitivityRuleControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var customSensitivityRuleService: CustomSensitivityRuleService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeRuleResponse(id: Long = 1L) = CustomSensitivityRuleResponse(
        id = id,
        name = "Internal_ID",
        description = "Matches internal ID columns",
        dataTypeFilter = GenericDataType.NUMERIC,
        matchers = listOf(
            CustomRuleMatcherDto(matcherType = MatcherType.CONTAINS, value = "uid", caseSensitive = false)
        ),
        linkedPresetId = null,
        isActive = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `GET list rules returns 200 with list`() {
        whenever(customSensitivityRuleService.listRules()).thenReturn(listOf(makeRuleResponse()))

        mockMvc.perform(get("/api/sensitivity-rules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Internal_ID"))
    }

    @Test
    fun `GET single rule returns 200`() {
        whenever(customSensitivityRuleService.getRule(1L)).thenReturn(makeRuleResponse())

        mockMvc.perform(get("/api/sensitivity-rules/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.dataTypeFilter").value("NUMERIC"))
    }

    @Test
    fun `GET single rule returns 404 when not found`() {
        whenever(customSensitivityRuleService.getRule(99L))
            .thenThrow(NoSuchElementException("Not found"))

        mockMvc.perform(get("/api/sensitivity-rules/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST create rule returns 201`() {
        whenever(customSensitivityRuleService.createRule(any())).thenReturn(makeRuleResponse())

        mockMvc.perform(
            post("/api/sensitivity-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"name":"Internal_ID","dataTypeFilter":"NUMERIC",
                    |"matchers":[{"matcherType":"CONTAINS","value":"uid","caseSensitive":false}]}""".trimMargin()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Internal_ID"))
    }

    @Test
    fun `PUT update rule returns 200`() {
        whenever(customSensitivityRuleService.updateRule(eq(1L), any())).thenReturn(makeRuleResponse())

        mockMvc.perform(
            put("/api/sensitivity-rules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Internal_ID","dataTypeFilter":"NUMERIC","matchers":[]}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    fun `DELETE rule returns 204`() {
        doNothing().whenever(customSensitivityRuleService).deleteRule(1L)

        mockMvc.perform(delete("/api/sensitivity-rules/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE rule returns 404 when not found`() {
        whenever(customSensitivityRuleService.deleteRule(99L))
            .thenThrow(NoSuchElementException("Not found"))

        mockMvc.perform(delete("/api/sensitivity-rules/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST preview returns 200 with matched columns`() {
        val results = listOf(
            CustomRulePreviewResult("users", "user_id", "integer"),
            CustomRulePreviewResult("transactions", "tx_id", "integer")
        )
        whenever(customSensitivityRuleService.previewRule(any())).thenReturn(results)

        mockMvc.perform(
            post("/api/sensitivity-rules/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"workspaceId":1,"dataTypeFilter":"NUMERIC",
                    |"matchers":[{"matcherType":"ENDS_WITH","value":"id","caseSensitive":false}]}""".trimMargin()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].tableName").value("users"))
            .andExpect(jsonPath("$[0].columnName").value("user_id"))
            .andExpect(jsonPath("$[1].columnName").value("tx_id"))
    }
}
